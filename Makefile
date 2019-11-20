#
# Makefile to set up consistent build environment for generated files
#

# specific versions to ensure consistent rebuilds
BLACK_VERSION = 19.10b0
PYQT5_VERSION = 5.13.1
TORCH_VERSION = 1.3
PROTOC_DIST = https://github.com/google/protobuf/releases/download/v3.0.0/protoc-3.0.0-linux-x86_64.zip


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
	python-client/design.py \
	protocol/openrtist_pb2.py

REQUIREMENTS = \
	'PyQT5==$(PYQT5_VERSION)' \
	'opencv-python' \
	'fire' \
	'torch==$(TORCH_VERSION)' \
	'torchvision' \
	'black==$(BLACK_VERSION)' \
	flake8 \
	flake8-bugbear

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
	# install protoc
	mkdir -p .venv/tmp
	wget -O .venv/tmp/protobuf.zip $(PROTOC_DIST)
	unzip -o .venv/tmp/protobuf.zip -d .venv bin/protoc
	touch .venv

%.py: %.ui .venv
	.venv/bin/pyuic5 -x $< -o $@
%_pb2.py: %.proto .venv
	cd $(dir $<) && $(PWD)/.venv/bin/protoc --python_out=. $(notdir $<)

$(LOCAL_EXEC_ASSET_DIR)/%.pt: models/%.model .venv
	mkdir -p $(LOCAL_EXEC_ASSET_DIR)
	.venv/bin/python scripts/freeze_model.py freeze --weight-file-path='$<' --output-file-path='$@'

.PHONY: all check reformat docker clean distclean
.PRECIOUS: $(GENERATED_FILES)
