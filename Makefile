#
# Makefile to set up consistent build environment for generated files
#

# specific versions to ensure consistent rebuilds
BLACK_VERSION = 22.6.0
PYQT5_VERSION = 5.14.2
TORCH_VERSION = 1.11.0 # originally 1.4.0
TORCHVISION_VERSION = 0.12.0 # originally 0.5.0


LOCAL_EXECUTION_MODELS = \
	cafe_gogh.model \
	candy.model \
	david_vaughan.model \
	dido_carthage.model \
	fall_icarus.model \
	femmes_d_alger.model \
	going_to_work.model \
	monet.model \
	mosaic.model \
	rain_princess.model \
	starry-night.model \
	sunday_afternoon.model \
	the_scream.model \
	udnie.model \
	weeping_woman.model

LOCAL_EXEC_ASSET_DIR = android-client/app/src/main/assets

GENERATED_FILES = \
	$(LOCAL_EXECUTION_MODELS:%.model=$(LOCAL_EXEC_ASSET_DIR)/%.pt) \
	python-client/src/openrtist/design.py \
	python-client/src/openrtist/openrtist_pb2.py

REQUIREMENTS = \
	'PyQT5==$(PYQT5_VERSION)' \
	'opencv-python' \
	'fire' \
	'torch==$(TORCH_VERSION)' \
	'torchvision==$(TORCHVISION_VERSION)' \
	'black==$(BLACK_VERSION)' \
	flake8 \
	flake8-bugbear \
	grpcio-tools

all: $(GENERATED_FILES)

check: .venv
	.venv/bin/black --check .
	.venv/bin/flake8

reformat: .venv
	.venv/bin/black .

docker: all
	docker build -t cmusatyalab/openrtist .

clean:
	$(RM) $(GENERATED_FILES)

distclean: clean
	$(RM) -r .venv

.venv:
	python3 -m venv .venv
	.venv/bin/pip install $(REQUIREMENTS)
	mkdir -p .venv/tmp
	touch .venv

%.py: %.ui .venv
	.venv/bin/pyuic5 -x $< -o $@

#update pip install grpcio-tools
python-client/src/openrtist/openrtist_pb2.py: android-client/app/src/main/proto/openrtist.proto .venv
	.venv/bin/python -m grpc_tools.protoc --python_out=server -I android-client/app/src/main/proto openrtist.proto
	.venv/bin/python -m grpc_tools.protoc --python_out=python-client/src/openrtist -I android-client/app/src/main/proto openrtist.proto

$(LOCAL_EXEC_ASSET_DIR)/%.pt: models/%.model .venv
	mkdir -p $(LOCAL_EXEC_ASSET_DIR)
	.venv/bin/python scripts/freeze_model.py freeze --weight-file-path='$<' --output-file-path='$@'

.PHONY: all check reformat docker clean distclean
.PRECIOUS: $(GENERATED_FILES)
