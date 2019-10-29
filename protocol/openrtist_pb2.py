# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: openrtist.proto

import sys
_b=sys.version_info[0]<3 and (lambda x:x) or (lambda x:x.encode('latin1'))
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from google.protobuf import reflection as _reflection
from google.protobuf import symbol_database as _symbol_database
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


from google.protobuf import wrappers_pb2 as google_dot_protobuf_dot_wrappers__pb2


DESCRIPTOR = _descriptor.FileDescriptor(
  name='openrtist.proto',
  package='openrtist',
  syntax='proto3',
  serialized_options=_b('\n\024edu.cmu.cs.openrtistB\006Protos'),
  serialized_pb=_b('\n\x0fopenrtist.proto\x12\topenrtist\x1a\x1egoogle/protobuf/wrappers.proto\"\xbd\x01\n\x0c\x45ngineFields\x12\r\n\x05style\x18\x01 \x01(\t\x12:\n\nstyle_list\x18\x02 \x03(\x0b\x32&.openrtist.EngineFields.StyleListEntry\x12\x30\n\x0bstyle_image\x18\x03 \x01(\x0b\x32\x1b.google.protobuf.BytesValue\x1a\x30\n\x0eStyleListEntry\x12\x0b\n\x03key\x18\x01 \x01(\t\x12\r\n\x05value\x18\x02 \x01(\t:\x02\x38\x01\x42\x1e\n\x14\x65\x64u.cmu.cs.openrtistB\x06Protosb\x06proto3')
  ,
  dependencies=[google_dot_protobuf_dot_wrappers__pb2.DESCRIPTOR,])




_ENGINEFIELDS_STYLELISTENTRY = _descriptor.Descriptor(
  name='StyleListEntry',
  full_name='openrtist.EngineFields.StyleListEntry',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='key', full_name='openrtist.EngineFields.StyleListEntry.key', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='value', full_name='openrtist.EngineFields.StyleListEntry.value', index=1,
      number=2, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[],
  enum_types=[
  ],
  serialized_options=_b('8\001'),
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=204,
  serialized_end=252,
)

_ENGINEFIELDS = _descriptor.Descriptor(
  name='EngineFields',
  full_name='openrtist.EngineFields',
  filename=None,
  file=DESCRIPTOR,
  containing_type=None,
  fields=[
    _descriptor.FieldDescriptor(
      name='style', full_name='openrtist.EngineFields.style', index=0,
      number=1, type=9, cpp_type=9, label=1,
      has_default_value=False, default_value=_b("").decode('utf-8'),
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='style_list', full_name='openrtist.EngineFields.style_list', index=1,
      number=2, type=11, cpp_type=10, label=3,
      has_default_value=False, default_value=[],
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
    _descriptor.FieldDescriptor(
      name='style_image', full_name='openrtist.EngineFields.style_image', index=2,
      number=3, type=11, cpp_type=10, label=1,
      has_default_value=False, default_value=None,
      message_type=None, enum_type=None, containing_type=None,
      is_extension=False, extension_scope=None,
      serialized_options=None, file=DESCRIPTOR),
  ],
  extensions=[
  ],
  nested_types=[_ENGINEFIELDS_STYLELISTENTRY, ],
  enum_types=[
  ],
  serialized_options=None,
  is_extendable=False,
  syntax='proto3',
  extension_ranges=[],
  oneofs=[
  ],
  serialized_start=63,
  serialized_end=252,
)

_ENGINEFIELDS_STYLELISTENTRY.containing_type = _ENGINEFIELDS
_ENGINEFIELDS.fields_by_name['style_list'].message_type = _ENGINEFIELDS_STYLELISTENTRY
_ENGINEFIELDS.fields_by_name['style_image'].message_type = google_dot_protobuf_dot_wrappers__pb2._BYTESVALUE
DESCRIPTOR.message_types_by_name['EngineFields'] = _ENGINEFIELDS
_sym_db.RegisterFileDescriptor(DESCRIPTOR)

EngineFields = _reflection.GeneratedProtocolMessageType('EngineFields', (_message.Message,), dict(

  StyleListEntry = _reflection.GeneratedProtocolMessageType('StyleListEntry', (_message.Message,), dict(
    DESCRIPTOR = _ENGINEFIELDS_STYLELISTENTRY,
    __module__ = 'openrtist_pb2'
    # @@protoc_insertion_point(class_scope:openrtist.EngineFields.StyleListEntry)
    ))
  ,
  DESCRIPTOR = _ENGINEFIELDS,
  __module__ = 'openrtist_pb2'
  # @@protoc_insertion_point(class_scope:openrtist.EngineFields)
  ))
_sym_db.RegisterMessage(EngineFields)
_sym_db.RegisterMessage(EngineFields.StyleListEntry)


DESCRIPTOR._options = None
_ENGINEFIELDS_STYLELISTENTRY._options = None
# @@protoc_insertion_point(module_scope)
