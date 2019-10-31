#
# Makefile to set up consistent build environment for generated files
#

BLACK_VERSION = 19.10b0
PYQT5_VERSION = 5.13.1
PROTOC_DIST = https://github.com/google/protobuf/releases/download/v3.0.0/protoc-3.0.0-linux-x86_64.zip

GENERATED_FILES = \
	gabriel-client-openrtist-python/design.py \
	protocol/openrtist_pb2.py

all: $(GENERATED_FILES) .venv
	.venv/bin/black --check . || true

clean:
	$(RM) $(GENERATED_FILES)

distclean: clean
	$(RM) -r .venv

docker: all
	git archive --format=tgz HEAD | docker build -t cmusatyalab/openrtist -

.venv:
	python3 -m venv .venv
	.venv/bin/pip install wheel 'black==$(BLACK_VERSION)' 'PyQt5==$(PYQT5_VERSION)'
	# install protoc
	mkdir -p .venv/tmp
	wget -O .venv/tmp/protobuf.zip $(PROTOC_DIST)
	unzip -o .venv/tmp/protobuf.zip -d .venv bin/protoc

%.py: %.ui .venv
	.venv/bin/pyuic5 -x $< -o $@

%_pb2.py: %.proto .venv
	cd $(dir $<) && $(PWD)/.venv/bin/protoc --python_out=. $(notdir $<)

.PHONY: all clean distclean docker
.PRECIOUS: $(GENERATED_FILES)
