// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: openrtist.proto

package edu.cmu.cs.openrtist;

public final class Protos {
  private Protos() {}
  public static void registerAllExtensions(
          com.google.protobuf.ExtensionRegistryLite registry) {
  }
  public interface EngineFieldsOrBuilder extends
          // @@protoc_insertion_point(interface_extends:openrtist.EngineFields)
          com.google.protobuf.MessageLiteOrBuilder {

    /**
     * <code>optional string style = 1;</code>
     */
    java.lang.String getStyle();
    /**
     * <code>optional string style = 1;</code>
     */
    com.google.protobuf.ByteString
    getStyleBytes();
  }
  /**
   * Protobuf type {@code openrtist.EngineFields}
   */
  public  static final class EngineFields extends
          com.google.protobuf.GeneratedMessageLite<
                  EngineFields, EngineFields.Builder> implements
          // @@protoc_insertion_point(message_implements:openrtist.EngineFields)
          EngineFieldsOrBuilder {
    private EngineFields() {
      style_ = "";
    }
    public static final int STYLE_FIELD_NUMBER = 1;
    private java.lang.String style_;
    /**
     * <code>optional string style = 1;</code>
     */
    public java.lang.String getStyle() {
      return style_;
    }
    /**
     * <code>optional string style = 1;</code>
     */
    public com.google.protobuf.ByteString
    getStyleBytes() {
      return com.google.protobuf.ByteString.copyFromUtf8(style_);
    }
    /**
     * <code>optional string style = 1;</code>
     */
    private void setStyle(
            java.lang.String value) {
      if (value == null) {
        throw new NullPointerException();
      }

      style_ = value;
    }
    /**
     * <code>optional string style = 1;</code>
     */
    private void clearStyle() {

      style_ = getDefaultInstance().getStyle();
    }
    /**
     * <code>optional string style = 1;</code>
     */
    private void setStyleBytes(
            com.google.protobuf.ByteString value) {
      if (value == null) {
        throw new NullPointerException();
      }
      checkByteStringIsUtf8(value);

      style_ = value.toStringUtf8();
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
            throws java.io.IOException {
      if (!style_.isEmpty()) {
        output.writeString(1, getStyle());
      }
    }

    public int getSerializedSize() {
      int size = memoizedSerializedSize;
      if (size != -1) return size;

      size = 0;
      if (!style_.isEmpty()) {
        size += com.google.protobuf.CodedOutputStream
                .computeStringSize(1, getStyle());
      }
      memoizedSerializedSize = size;
      return size;
    }

    public static edu.cmu.cs.openrtist.Protos.EngineFields parseFrom(
            com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, data);
    }
    public static edu.cmu.cs.openrtist.Protos.EngineFields parseFrom(
            com.google.protobuf.ByteString data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, data, extensionRegistry);
    }
    public static edu.cmu.cs.openrtist.Protos.EngineFields parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, data);
    }
    public static edu.cmu.cs.openrtist.Protos.EngineFields parseFrom(
            byte[] data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, data, extensionRegistry);
    }
    public static edu.cmu.cs.openrtist.Protos.EngineFields parseFrom(java.io.InputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, input);
    }
    public static edu.cmu.cs.openrtist.Protos.EngineFields parseFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, input, extensionRegistry);
    }
    public static edu.cmu.cs.openrtist.Protos.EngineFields parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
      return parseDelimitedFrom(DEFAULT_INSTANCE, input);
    }
    public static edu.cmu.cs.openrtist.Protos.EngineFields parseDelimitedFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
    }
    public static edu.cmu.cs.openrtist.Protos.EngineFields parseFrom(
            com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, input);
    }
    public static edu.cmu.cs.openrtist.Protos.EngineFields parseFrom(
            com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
      return com.google.protobuf.GeneratedMessageLite.parseFrom(
              DEFAULT_INSTANCE, input, extensionRegistry);
    }

    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(edu.cmu.cs.openrtist.Protos.EngineFields prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    /**
     * Protobuf type {@code openrtist.EngineFields}
     */
    public static final class Builder extends
            com.google.protobuf.GeneratedMessageLite.Builder<
                    edu.cmu.cs.openrtist.Protos.EngineFields, Builder> implements
            // @@protoc_insertion_point(builder_implements:openrtist.EngineFields)
            edu.cmu.cs.openrtist.Protos.EngineFieldsOrBuilder {
      // Construct using edu.cmu.cs.openrtist.Protos.EngineFields.newBuilder()
      private Builder() {
        super(DEFAULT_INSTANCE);
      }


      /**
       * <code>optional string style = 1;</code>
       */
      public java.lang.String getStyle() {
        return instance.getStyle();
      }
      /**
       * <code>optional string style = 1;</code>
       */
      public com.google.protobuf.ByteString
      getStyleBytes() {
        return instance.getStyleBytes();
      }
      /**
       * <code>optional string style = 1;</code>
       */
      public Builder setStyle(
              java.lang.String value) {
        copyOnWrite();
        instance.setStyle(value);
        return this;
      }
      /**
       * <code>optional string style = 1;</code>
       */
      public Builder clearStyle() {
        copyOnWrite();
        instance.clearStyle();
        return this;
      }
      /**
       * <code>optional string style = 1;</code>
       */
      public Builder setStyleBytes(
              com.google.protobuf.ByteString value) {
        copyOnWrite();
        instance.setStyleBytes(value);
        return this;
      }

      // @@protoc_insertion_point(builder_scope:openrtist.EngineFields)
    }
    protected final Object dynamicMethod(
            com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
            Object arg0, Object arg1) {
      switch (method) {
        case NEW_MUTABLE_INSTANCE: {
          return new edu.cmu.cs.openrtist.Protos.EngineFields();
        }
        case IS_INITIALIZED: {
          return DEFAULT_INSTANCE;
        }
        case MAKE_IMMUTABLE: {
          return null;
        }
        case NEW_BUILDER: {
          return new Builder();
        }
        case VISIT: {
          Visitor visitor = (Visitor) arg0;
          edu.cmu.cs.openrtist.Protos.EngineFields other = (edu.cmu.cs.openrtist.Protos.EngineFields) arg1;
          style_ = visitor.visitString(!style_.isEmpty(), style_,
                  !other.style_.isEmpty(), other.style_);
          if (visitor == com.google.protobuf.GeneratedMessageLite.MergeFromVisitor
                  .INSTANCE) {
          }
          return this;
        }
        case MERGE_FROM_STREAM: {
          com.google.protobuf.CodedInputStream input =
                  (com.google.protobuf.CodedInputStream) arg0;
          com.google.protobuf.ExtensionRegistryLite extensionRegistry =
                  (com.google.protobuf.ExtensionRegistryLite) arg1;
          try {
            boolean done = false;
            while (!done) {
              int tag = input.readTag();
              switch (tag) {
                case 0:
                  done = true;
                  break;
                default: {
                  if (!input.skipField(tag)) {
                    done = true;
                  }
                  break;
                }
                case 10: {
                  String s = input.readStringRequireUtf8();

                  style_ = s;
                  break;
                }
              }
            }
          } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            throw new RuntimeException(e.setUnfinishedMessage(this));
          } catch (java.io.IOException e) {
            throw new RuntimeException(
                    new com.google.protobuf.InvalidProtocolBufferException(
                            e.getMessage()).setUnfinishedMessage(this));
          } finally {
          }
        }
        case GET_DEFAULT_INSTANCE: {
          return DEFAULT_INSTANCE;
        }
        case GET_PARSER: {
          if (PARSER == null) {    synchronized (edu.cmu.cs.openrtist.Protos.EngineFields.class) {
            if (PARSER == null) {
              PARSER = new DefaultInstanceBasedParser(DEFAULT_INSTANCE);
            }
          }
          }
          return PARSER;
        }
      }
      throw new UnsupportedOperationException();
    }


    // @@protoc_insertion_point(class_scope:openrtist.EngineFields)
    private static final edu.cmu.cs.openrtist.Protos.EngineFields DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new EngineFields();
      DEFAULT_INSTANCE.makeImmutable();
    }

    public static edu.cmu.cs.openrtist.Protos.EngineFields getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static volatile com.google.protobuf.Parser<EngineFields> PARSER;

    public static com.google.protobuf.Parser<EngineFields> parser() {
      return DEFAULT_INSTANCE.getParserForType();
    }
  }


  static {
  }

  // @@protoc_insertion_point(outer_class_scope)
}
