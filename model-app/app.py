import os
import time
from zipfile import ZipFile
from flask import Flask, flash, jsonify, render_template, request, redirect, send_from_directory, url_for
from werkzeug.utils import secure_filename
from .openvino_convert import convert
from .make_celery import make_celery
from .train_style import train, get_args

ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif'}

app = Flask(__name__)
app.config.from_pyfile('config.py')
app.config['MAX_CONTENT_LENGTH'] = 16 * 1024 * 1024
celery = make_celery(app)


def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in ALLOWED_EXTENSIONS


@app.route('/', methods=['GET'])
@app.route('/<task_id>', methods=['GET'])
def index(task_id=None):
    return render_template('index.html', task_id=task_id)


@app.route('/', methods=['POST'])
@app.route('/<task_id>', methods=['POST'])
def upload_file(task_id=None):
    # check if the post request has the file part
    if 'file' not in request.files:
        flash('No file part')
        return redirect(request.url)
    file = request.files['file']

    # if user does not select file, browser also
    # submit an empty part without filename
    if file.filename == '':
        flash('No selected file')
        return redirect(request.url)

    if not allowed_file(file.filename):
        flash('File extension not supported (allowed extensions: png, jpg, jpeg, gif)')
        return redirect(request.url)

    filename = secure_filename(file.filename)
    filepath = os.path.join(app.config['UPLOAD_FOLDER'], filename)
    file.save(filepath)
    task = run_training.delay(app.config['DATASET'], filepath, app.config['DOWNLOAD_FOLDER'], filename)
    return redirect(url_for('index', task_id=task.id))


@app.route('/status/<task_id>')
def train_status(task_id):
    task = run_training.AsyncResult(task_id)

    if task.state == 'FAILURE' or task.state == 'REVOKED':
        response = {
            'state': task.state,
            'current': 1,
            'total': 1,
            'status': str(task.info)
        }
    else:
        response = {
            'state': task.state,
            'current': task.info.get('current', 0),
            'total': task.info.get('total', 1),
            'status': task.info.get('status', ''),
            'start_time': task.info.get('start_time', ''),
            'style': task.info.get('style', ''),
        }
        if 'model' in task.info:
            response['model'] = task.info['model']

    return jsonify(response)


@app.route('/models/<filename>')
def get_model(filename):
    return send_from_directory(app.config['DOWNLOAD_FOLDER'], filename)

@app.route('/styles/<filename>')
def get_style(filename):
    return send_from_directory(app.config['UPLOAD_FOLDER'], filename)

@app.route('/cancel', methods=['POST'])
def cancel_train():
    task_id = request.form['task_id']
    run_training.AsyncResult(task_id).revoke(terminate=True)
    return redirect(url_for('index', task_id=task_id))

@celery.task(bind=True)
def run_training(self, dataset, filepath, model_dir, image):
    start_time = int(round(time.time() * 1000))
    self.update_state(meta={'start_time': start_time, 'style': image})

    def log_progress(epoch, num_epochs, count, num_images, content, style, flicker, total):
        message = "Epoch {}:\t[{}/{}]\tcontent: {:.6f}\tstyle: {:.6f}\tflicker: {:.6f}\ttotal: {:.6f}\n".format(
            epoch + 1, count, num_images, content, style, flicker, total
        )
        self.update_state(meta={'current': count + epoch * num_images, 'total': num_images * num_epochs,
                                'status': message, 'start_time': start_time, 'style': image})
    model = train(get_args([
        '--dataset', dataset,
        '--style-image', filepath,
        '--save-model-dir', model_dir,
        '--log-interval', '10',
        '--style-size', '512'
    ]), log_progress)

    if app.config['CONVERT_TO_OPEN_VINO']:
        pytorch_model = os.path.join(app.config['DOWNLOAD_FOLDER'], model)
        convert(pytorch_model)
        model_name = model[:-len(".model")]
        archive = model_name + '.zip'
        with ZipFile(os.path.join(app.config['DOWNLOAD_FOLDER'], archive), 'w') as zip_file:
            zip_file.write(pytorch_model, model)
            vino_xml = model_name + '.xml'
            zip_file.write(os.path.join(app.config['DOWNLOAD_FOLDER'], vino_xml), vino_xml)
            vino_bin = model_name + '.bin'
            zip_file.write(os.path.join(app.config['DOWNLOAD_FOLDER'], vino_bin), vino_bin)
            zip_file.close()
        to_return = archive
    else:
        to_return = model

    return {
        'current': 100,
        'total': 100,
        'start_time': start_time,
        'end_time': round(time.time() * 1000),
        'model': to_return,
        'style': image
    }
