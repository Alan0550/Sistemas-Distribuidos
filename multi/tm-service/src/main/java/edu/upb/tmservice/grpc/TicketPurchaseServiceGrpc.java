package edu.upb.tmservice.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.62.2)",
    comments = "Source: ticket_purchase.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class TicketPurchaseServiceGrpc {

  private TicketPurchaseServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "tmservice.TicketPurchaseService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<edu.upb.tmservice.grpc.CompraTicketRequest,
      edu.upb.tmservice.grpc.CompraTicketResponse> getComprarTicketMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "comprarTicket",
      requestType = edu.upb.tmservice.grpc.CompraTicketRequest.class,
      responseType = edu.upb.tmservice.grpc.CompraTicketResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<edu.upb.tmservice.grpc.CompraTicketRequest,
      edu.upb.tmservice.grpc.CompraTicketResponse> getComprarTicketMethod() {
    io.grpc.MethodDescriptor<edu.upb.tmservice.grpc.CompraTicketRequest, edu.upb.tmservice.grpc.CompraTicketResponse> getComprarTicketMethod;
    if ((getComprarTicketMethod = TicketPurchaseServiceGrpc.getComprarTicketMethod) == null) {
      synchronized (TicketPurchaseServiceGrpc.class) {
        if ((getComprarTicketMethod = TicketPurchaseServiceGrpc.getComprarTicketMethod) == null) {
          TicketPurchaseServiceGrpc.getComprarTicketMethod = getComprarTicketMethod =
              io.grpc.MethodDescriptor.<edu.upb.tmservice.grpc.CompraTicketRequest, edu.upb.tmservice.grpc.CompraTicketResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "comprarTicket"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  edu.upb.tmservice.grpc.CompraTicketRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  edu.upb.tmservice.grpc.CompraTicketResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TicketPurchaseServiceMethodDescriptorSupplier("comprarTicket"))
              .build();
        }
      }
    }
    return getComprarTicketMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static TicketPurchaseServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TicketPurchaseServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TicketPurchaseServiceStub>() {
        @java.lang.Override
        public TicketPurchaseServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TicketPurchaseServiceStub(channel, callOptions);
        }
      };
    return TicketPurchaseServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static TicketPurchaseServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TicketPurchaseServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TicketPurchaseServiceBlockingStub>() {
        @java.lang.Override
        public TicketPurchaseServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TicketPurchaseServiceBlockingStub(channel, callOptions);
        }
      };
    return TicketPurchaseServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static TicketPurchaseServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<TicketPurchaseServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<TicketPurchaseServiceFutureStub>() {
        @java.lang.Override
        public TicketPurchaseServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new TicketPurchaseServiceFutureStub(channel, callOptions);
        }
      };
    return TicketPurchaseServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void comprarTicket(edu.upb.tmservice.grpc.CompraTicketRequest request,
        io.grpc.stub.StreamObserver<edu.upb.tmservice.grpc.CompraTicketResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getComprarTicketMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service TicketPurchaseService.
   */
  public static abstract class TicketPurchaseServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return TicketPurchaseServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service TicketPurchaseService.
   */
  public static final class TicketPurchaseServiceStub
      extends io.grpc.stub.AbstractAsyncStub<TicketPurchaseServiceStub> {
    private TicketPurchaseServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TicketPurchaseServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TicketPurchaseServiceStub(channel, callOptions);
    }

    /**
     */
    public void comprarTicket(edu.upb.tmservice.grpc.CompraTicketRequest request,
        io.grpc.stub.StreamObserver<edu.upb.tmservice.grpc.CompraTicketResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getComprarTicketMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service TicketPurchaseService.
   */
  public static final class TicketPurchaseServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<TicketPurchaseServiceBlockingStub> {
    private TicketPurchaseServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TicketPurchaseServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TicketPurchaseServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public edu.upb.tmservice.grpc.CompraTicketResponse comprarTicket(edu.upb.tmservice.grpc.CompraTicketRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getComprarTicketMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service TicketPurchaseService.
   */
  public static final class TicketPurchaseServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<TicketPurchaseServiceFutureStub> {
    private TicketPurchaseServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TicketPurchaseServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new TicketPurchaseServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<edu.upb.tmservice.grpc.CompraTicketResponse> comprarTicket(
        edu.upb.tmservice.grpc.CompraTicketRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getComprarTicketMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_COMPRAR_TICKET = 0;

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
        case METHODID_COMPRAR_TICKET:
          serviceImpl.comprarTicket((edu.upb.tmservice.grpc.CompraTicketRequest) request,
              (io.grpc.stub.StreamObserver<edu.upb.tmservice.grpc.CompraTicketResponse>) responseObserver);
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
          getComprarTicketMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              edu.upb.tmservice.grpc.CompraTicketRequest,
              edu.upb.tmservice.grpc.CompraTicketResponse>(
                service, METHODID_COMPRAR_TICKET)))
        .build();
  }

  private static abstract class TicketPurchaseServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    TicketPurchaseServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return edu.upb.tmservice.grpc.TicketPurchase.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("TicketPurchaseService");
    }
  }

  private static final class TicketPurchaseServiceFileDescriptorSupplier
      extends TicketPurchaseServiceBaseDescriptorSupplier {
    TicketPurchaseServiceFileDescriptorSupplier() {}
  }

  private static final class TicketPurchaseServiceMethodDescriptorSupplier
      extends TicketPurchaseServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    TicketPurchaseServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (TicketPurchaseServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new TicketPurchaseServiceFileDescriptorSupplier())
              .addMethod(getComprarTicketMethod())
              .build();
        }
      }
    }
    return result;
  }
}
