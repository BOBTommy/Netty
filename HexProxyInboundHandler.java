package ProxyServer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;

public class HexProxyInboundHandler extends SimpleChannelUpstreamHandler{

	private final ClientSocketChannelFactory cf;
	private final String remoteHost;
	private	final int remotePort;
	
	final Object trafficLock = new Object();
	private volatile Channel outboundChannel;
	
	public HexProxyInboundHandler(ClientSocketChannelFactory cf, String remoteHost, int remotePort)
	{
		this.cf=cf;
		this.remoteHost=remoteHost;
		this.remotePort=remotePort;
	}
	
	@Override
	public void channelOpen(ChannelHandlerContext ctx,ChannelStateEvent e)
	throws Exception{
		final Channel inboundChannel = e.getChannel();
		inboundChannel.setReadable(false);
		
		ClientBootstrap cb = new ClientBootstrap(cf);
		cb.getPipeline().addLast("handler", new OutboundHandler(e.getChannel()));
		ChannelFuture f = cb.connect(new InetSocketAddress(remoteHost, remotePort));
		
		outboundChannel = f.getChannel();
		f.addListener(new ChannelFutureListener() {
			
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()){
					inboundChannel.setReadable(true);
				}else{
					inboundChannel.close();
				}
				
			}
		});
	}
	
	@Override
	public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e){
		ChannelBuffer msg = (ChannelBuffer) e.getMessage();
		BigEndianHeapChannelBuffer buf = (BigEndianHeapChannelBuffer) e.getMessage();
		String tmp = buf.toString(Charset.defaultCharset());
		tmp.replaceAll("/forum/5", "/forum/6");
		ChannelBuffer b = (ChannelBuffer) ChannelBuffers.copiedBuffer(tmp.getBytes(Charset.defaultCharset()));
		
		synchronized (trafficLock) {
			outboundChannel.write(b);
			
			if(!outboundChannel.isWritable()){
				if(outboundChannel != null){
					outboundChannel.setReadable(true);
				}
			}
		}
	}
	
	@Override
	public void channelInterestChanged(ChannelHandlerContext ctx,
			ChannelStateEvent e){
		synchronized (trafficLock) {
			if(e.getChannel().isWritable()){
				if(outboundChannel != null)
				{
					outboundChannel.setReadable(true);
				}
			}
		}
	}
	
	@Override
	public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e){
		if(outboundChannel != null){
			closeOnFlush(outboundChannel);
		}
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e){
		e.getCause().printStackTrace();
		closeOnFlush(e.getChannel());
	}
	
	static void closeOnFlush(Channel ch){
		if(ch.isConnected()){
			ch.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	
	//Big OutBound Channel Handler
	
	private class OutboundHandler extends SimpleChannelUpstreamHandler{
		private final Channel inboundChannel;
		
		public OutboundHandler(Channel inboundChannel){
			this.inboundChannel = inboundChannel;
		}
		
		@Override
		public void messageReceived(ChannelHandlerContext ctx, final MessageEvent e){
			ChannelBuffer msg = (ChannelBuffer) e.getMessage();
			//Message 조작
			BigEndianHeapChannelBuffer buf = (BigEndianHeapChannelBuffer) e.getMessage();
			File file = new File("c:/test.txt");
			ChannelBuffer tmp;
			String strTmp = buf.toString(Charset.defaultCharset());
			if(strTmp.contains("개발 작업 도중 일어난 문제점을 서로 상의하세요")) {
				System.out.println("We Found String!!");
				System.out.println(strTmp);
				String result;
				result = strTmp.replaceAll("개발 작업 도중 일어난 문제점을 서로 상의하세요", "물어보지 마요 현기증 난단말이에요");
				//strTmp.replaceAll("KLDP", "BOBTommy");
				System.out.println(result);
				tmp = (ChannelBuffer) ChannelBuffers.copiedBuffer(ByteOrder.BIG_ENDIAN,result.getBytes());
			}else{tmp = (ChannelBuffer) ChannelBuffers.copiedBuffer(ByteOrder.BIG_ENDIAN,strTmp.getBytes());}
	//		System.out.println(strTmp);
			
	//		System.out.println(tmp.toString(Charset.defaultCharset()));
				//FileWriter fw = new FileWriter(file);
				//fw.write(buf.toString(Charset.defaultCharset()));
				//fw.flush();
				//fw.close();
			
			//System.out.println(buf.toString(Charset.defaultCharset()));
			
			/*
			HttpRequest req = (HttpRequest) e.getMessage();
			ChannelBuffer content = req.getContent();
			System.out.println(req.getUri());
			System.out.println(content.toString(Charset.defaultCharset()));
			*/
			synchronized (trafficLock){
				inboundChannel.write(tmp);
				if(!inboundChannel.isWritable()){
					e.getChannel().setReadable(false);
				}
			}
		}
		
		@Override
		public void channelInterestChanged( ChannelHandlerContext ctx, 
				ChannelStateEvent e){
			synchronized(trafficLock){
				if(e.getChannel().isWritable()){
					inboundChannel.setReadable(true);
				}
			}
		}
		
		@Override
		public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e){
			closeOnFlush(inboundChannel);
		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e){
		//	e.getCause().printStackTrace();
			closeOnFlush(e.getChannel());
		}
		
	}
}
