package HeaderServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;


public class GetHeaderServer {
	
	public static void main(String args[]){	
		ChannelFactory factory = 
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool());
		
		ServerBootstrap bootstrap = new ServerBootstrap(factory);
		
		GetHeaderServerHandler handler = new GetHeaderServerHandler();
		ChannelPipeline pipeline = bootstrap.getPipeline();
		pipeline.addLast("handler", handler);
		
		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);
		bootstrap.bind(new InetSocketAddress(8000));
		
	
	}
}
