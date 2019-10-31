
GENERATED_FILES = \
	gabriel-client-openrtist-python/design.py \
	protocol/openrtist_pb2.py

all: .venv $(GENERATED_FILES)
	.venv/bin/black --check . || true

clean:

distclean: clean
	$(RM) -r .venv

maintainer-clean: distclean
	$(RM) $(GENERATED_FILES)

docker: all
	git archive --format=tgz HEAD | docker build -t cmusatyalab/openrtist -


.venv:
	python3 -m venv .venv
	.venv/bin/pip install wheel 'black==19.10b0'

%.py: %.ui
	pyuic5 -o $@ $^

%_pb2.py: %.proto
	protoc --python_out=. $^

.PHONY: all clean distclean maintainer-clean docker
.PRECIOUS: $(GENERATED_FILES)
