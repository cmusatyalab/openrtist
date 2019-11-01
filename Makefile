#
# Makefile to set up consistent build environment for generated files
#

BLACK_VERSION = 19.10b0
PYQT5_VERSION = 5.13.1
PROTOC_DIST = https://github.com/google/protobuf/releases/download/v3.0.0/protoc-3.0.0-linux-x86_64.zip

GENERATED_FILES = \
	gabriel-client-openrtist-python/design.py \
	protocol/openrtist_pb2.py

REQUIREMENTS = \
	'PyQT5==$(PYQT5_VERSION)' \
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

%.py: %.ui .venv
	.venv/bin/pyuic5 -x $< -o $@

%_pb2.py: %.proto .venv
	cd $(dir $<) && $(PWD)/.venv/bin/protoc --python_out=. $(notdir $<)

.PHONY: all check reformat docker clean distclean
.PRECIOUS: $(GENERATED_FILES)
