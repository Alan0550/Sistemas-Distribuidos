package edu.upb.tickmaster.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.62.2)",
    comments = "Source: producto.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ProductoServiceGrpc {

  private ProductoServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "tickmaster.ProductoService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<edu.upb.tickmaster.grpc.ProductoRequest,
      edu.upb.tickmaster.grpc.ProductoResponse> getRegistrarProductoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "registrarProducto",
      requestType = edu.upb.tickmaster.grpc.ProductoRequest.class,
      responseType = edu.upb.tickmaster.grpc.ProductoResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<edu.upb.tickmaster.grpc.ProductoRequest,
      edu.upb.tickmaster.grpc.ProductoResponse> getRegistrarProductoMethod() {
    io.grpc.MethodDescriptor<edu.upb.tickmaster.grpc.ProductoRequest, edu.upb.tickmaster.grpc.ProductoResponse> getRegistrarProductoMethod;
    if ((getRegistrarProductoMethod = ProductoServiceGrpc.getRegistrarProductoMethod) == null) {
      synchronized (ProductoServiceGrpc.class) {
        if ((getRegistrarProductoMethod = ProductoServiceGrpc.getRegistrarProductoMethod) == null) {
          ProductoServiceGrpc.getRegistrarProductoMethod = getRegistrarProductoMethod =
              io.grpc.MethodDescriptor.<edu.upb.tickmaster.grpc.ProductoRequest, edu.upb.tickmaster.grpc.ProductoResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "registrarProducto"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  edu.upb.tickmaster.grpc.ProductoRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  edu.upb.tickmaster.grpc.ProductoResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ProductoServiceMethodDescriptorSupplier("registrarProducto"))
              .build();
        }
      }
    }
    return getRegistrarProductoMethod;
  }

  private static volatile io.grpc.MethodDescriptor<edu.upb.tickmaster.grpc.IdRequest,
      edu.upb.tickmaster.grpc.ProductoRequest> getObtenerProductoMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "obtenerProducto",
      requestType = edu.upb.tickmaster.grpc.IdRequest.class,
      responseType = edu.upb.tickmaster.grpc.ProductoRequest.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<edu.upb.tickmaster.grpc.IdRequest,
      edu.upb.tickmaster.grpc.ProductoRequest> getObtenerProductoMethod() {
    io.grpc.MethodDescriptor<edu.upb.tickmaster.grpc.IdRequest, edu.upb.tickmaster.grpc.ProductoRequest> getObtenerProductoMethod;
    if ((getObtenerProductoMethod = ProductoServiceGrpc.getObtenerProductoMethod) == null) {
      synchronized (ProductoServiceGrpc.class) {
        if ((getObtenerProductoMethod = ProductoServiceGrpc.getObtenerProductoMethod) == null) {
          ProductoServiceGrpc.getObtenerProductoMethod = getObtenerProductoMethod =
              io.grpc.MethodDescriptor.<edu.upb.tickmaster.grpc.IdRequest, edu.upb.tickmaster.grpc.ProductoRequest>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "obtenerProducto"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  edu.upb.tickmaster.grpc.IdRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  edu.upb.tickmaster.grpc.ProductoRequest.getDefaultInstance()))
              .setSchemaDescriptor(new ProductoServiceMethodDescriptorSupplier("obtenerProducto"))
              .build();
        }
      }
    }
    return getObtenerProductoMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ProductoServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ProductoServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ProductoServiceStub>() {
        @java.lang.Override
        public ProductoServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ProductoServiceStub(channel, callOptions);
        }
      };
    return ProductoServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ProductoServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ProductoServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ProductoServiceBlockingStub>() {
        @java.lang.Override
        public ProductoServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ProductoServiceBlockingStub(channel, callOptions);
        }
      };
    return ProductoServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ProductoServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ProductoServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ProductoServiceFutureStub>() {
        @java.lang.Override
        public ProductoServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ProductoServiceFutureStub(channel, callOptions);
        }
      };
    return ProductoServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void registrarProducto(edu.upb.tickmaster.grpc.ProductoRequest request,
        io.grpc.stub.StreamObserver<edu.upb.tickmaster.grpc.ProductoResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRegistrarProductoMethod(), responseObserver);
    }

    /**
     */
    default void obtenerProducto(edu.upb.tickmaster.grpc.IdRequest request,
        io.grpc.stub.StreamObserver<edu.upb.tickmaster.grpc.ProductoRequest> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getObtenerProductoMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ProductoService.
   */
  public static abstract class ProductoServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ProductoServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ProductoService.
   */
  public static final class ProductoServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ProductoServiceStub> {
    private ProductoServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ProductoServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ProductoServiceStub(channel, callOptions);
    }

    /**
     */
    public void registrarProducto(edu.upb.tickmaster.grpc.ProductoRequest request,
        io.grpc.stub.StreamObserver<edu.upb.tickmaster.grpc.ProductoResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRegistrarProductoMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void obtenerProducto(edu.upb.tickmaster.grpc.IdRequest request,
        io.grpc.stub.StreamObserver<edu.upb.tickmaster.grpc.ProductoRequest> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getObtenerProductoMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ProductoService.
   */
  public static final class ProductoServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ProductoServiceBlockingStub> {
    private ProductoServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ProductoServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ProductoServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public edu.upb.tickmaster.grpc.ProductoResponse registrarProducto(edu.upb.tickmaster.grpc.ProductoRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRegistrarProductoMethod(), getCallOptions(), request);
    }

    /**
     */
    public edu.upb.tickmaster.grpc.ProductoRequest obtenerProducto(edu.upb.tickmaster.grpc.IdRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getObtenerProductoMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ProductoService.
   */
  public static final class ProductoServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ProductoServiceFutureStub> {
    private ProductoServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ProductoServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ProductoServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<edu.upb.tickmaster.grpc.ProductoResponse> registrarProducto(
        edu.upb.tickmaster.grpc.ProductoRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRegistrarProductoMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<edu.upb.tickmaster.grpc.ProductoRequest> obtenerProducto(
        edu.upb.tickmaster.grpc.IdRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getObtenerProductoMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_REGISTRAR_PRODUCTO = 0;
  private static final int METHODID_OBTENER_PRODUCTO = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_REGISTRAR_PRODUCTO:
          serviceImpl.registrarProducto((edu.upb.tickmaster.grpc.ProductoRequest) request,
              (io.grpc.stub.StreamObserver<edu.upb.tickmaster.grpc.ProductoResponse>) responseObserver);
          break;
        case METHODID_OBTENER_PRODUCTO:
          serviceImpl.obtenerProducto((edu.upb.tickmaster.grpc.IdRequest) request,
              (io.grpc.stub.StreamObserver<edu.upb.tickmaster.grpc.ProductoRequest>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getRegistrarProductoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              edu.upb.tickmaster.grpc.ProductoRequest,
              edu.upb.tickmaster.grpc.ProductoResponse>(
                service, METHODID_REGISTRAR_PRODUCTO)))
        .addMethod(
          getObtenerProductoMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              edu.upb.tickmaster.grpc.IdRequest,
              edu.upb.tickmaster.grpc.ProductoRequest>(
                service, METHODID_OBTENER_PRODUCTO)))
        .build();
  }

  private static abstract class ProductoServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ProductoServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return edu.upb.tickmaster.grpc.Producto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ProductoService");
    }
  }

  private static final class ProductoServiceFileDescriptorSupplier
      extends ProductoServiceBaseDescriptorSupplier {
    ProductoServiceFileDescriptorSupplier() {}
  }

  private static final class ProductoServiceMethodDescriptorSupplier
      extends ProductoServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ProductoServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ProductoServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ProductoServiceFileDescriptorSupplier())
              .addMethod(getRegistrarProductoMethod())
              .addMethod(getObtenerProductoMethod())
              .build();
        }
      }
    }
    return result;
  }
}
