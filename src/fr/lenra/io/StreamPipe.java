package fr.lenra.io;

/*
 * Copyright 2017 Lenra.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the MIT Licence.
 *
 * Please contact Lenra, contact@lenra.fr or visit www.lenra.fr if you need additional information or
 * have any questions.
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * A stream pipe throw all the data from an input stream to an output stream.
 */
public class StreamPipe {
	private DataCopier copier = null;
	
	/**
	 * Creates a <code>StreamPipe</code> so that it is not yet {@linkplain #connect(InputStream, OutputStream) connected}.
	 * It must {@linkplain fr.lenra.io.StreamPipe#connect(InputStream, OutputStream) connect} an <code>InputStream</code> and an <code>OutputStream</code> before being used.
	 */
	public StreamPipe() {
		
	}
	
	/**
	 * Creates a <code>StreamPipe</code> connecting the specified input stream and output stream.
	 * All the data coming through <code>in</code> will then be writen to <code>out</code>.
	 * 
	 * @param in The input stream.
	 * @param out The output stream.
	 * @exception IOException If an I/O error occurs.
	 */
	public StreamPipe(InputStream in, OutputStream out) throws IOException {
		connect(in, out);
	}
	
	/**
	 * Connects the specified input stream and output stream.
	 * All the data coming through <code>in</code> will then be writen to <code>out</code>.
	 * 
	 * @param in The input stream.
	 * @param out The output stream.
	 * @exception IOException If an I/O error occurs.
	 */
	public void connect(InputStream in, OutputStream out) throws IOException {
		if (in==null || out==null)
			throw new NullPointerException();
		else if (copier!=null)
			throw new IOException("Already connected");
		this.copier = new DataCopier(in, out);
	}
	
	public void disconnect() {
		copier.disconnect();
	}
	
	private static class DataCopier extends Thread {
		private boolean disconnected = false;
		private InputStream in;
		private OutputStream out;
		public boolean endReached = false;
		public boolean inClosed = false;
		public boolean outClosed = false;
		
		public DataCopier(InputStream in, OutputStream out) {
			this.in = in;
			this.out = out;
			this.start();
		}
		
		@Override
		public synchronized void run() {
			byte[] buf = new byte[1024];
			
			while (!(disconnected || inClosed || outClosed || endReached)) {
				int len = 0;
				try {
					len = in.read(buf);
					if (len==-1) {
						endReached = true;
						continue;
					}
				}
				catch (IOException e) {
					inClosed = true;
					continue;
				}
				try {
					out.write(buf, 0, len);
				}
				catch (IOException e) {
					outClosed = true;
					continue;
				}
			}
			try {
				if (!outClosed)
					out.flush();
				if (!inClosed)
					in.close();
				if (!outClosed)
					out.close();
			} 
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public synchronized void disconnect() {
			disconnected = true;
		}
	}
}
