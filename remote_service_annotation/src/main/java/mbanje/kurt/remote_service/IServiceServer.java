package mbanje.kurt.remote_service;

/**
 * Created by kurt on 23 07 2015 .
 */
public interface IServiceServer {
    @RemoteMessageServer({
            IServiceClient.CONNECT,
            IServiceClient.DISCONNECT,
            IServiceClient.SHUTDOWN
    })
    void onBoundServiceConnectionChanged(boolean connected);
}
