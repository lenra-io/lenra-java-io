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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A piped input stream should be connected to a piped output stream; the piped input stream then provides whatever data bytes are written to the piped output stream.
 * Typically, data is read from a <code>PipedInputStream</code> object by one thread and data is written to the corresponding <code>PipedOutputStream</code> by some other thread.
 * Attempting to use both objects from a single thread is not recommended, as it may deadlock the thread.
 * The piped input stream contains a buffer, decoupling read operations from write operations, within limits.
 * A pipe is said to be <a name=BROKEN><i>broken</i></a> if a thread that was providing data bytes to the connected piped output stream is no longer alive.
 * 
 * @see fr.lenra.io.PipedOutputStream
 */
public class PipedInputStream extends InputStream {
	boolean closedByWriter = false;
	volatile boolean closedByReader = false;
	boolean connected = false;

	private int available = 0;
	private int inSize = 0;

	private int waiting = 0;

	private ByteArrayInputStream in = null;
	private ByteArrayOutputStream out = null;

	/**
	 * Creates a <code>PipedInputStream</code> so that it is not yet {@linkplain #connect(fr.lenra.io.PipedOutputStream) connected}.
	 * It must be {@linkplain fr.lenra.io.PipedOutputStream#connect(fr.lenra.io.PipedInputStream) connected} to a <code>PipedOutputStream</code> before being used.
	 */
	public PipedInputStream() {

	}

	/**
	 * Creates a <code>PipedInputStream</code> so that it is connected to the piped output stream <code>out</code>.
	 * Data bytes written to <code>out</code> will then be available as input from this stream.
	 * 
	 * @param out The stream to connect to.
	 * @exception IOException If an I/O error occurs.
	 */
	public PipedInputStream(PipedOutputStream out) throws IOException {
		connect(out);
	}

	/**
	 * Causes this piped input stream to be connected to the piped output stream
	 * <code>out</code>.
	 * If this object is already connected to some other piped
	 * output stream, an <code>IOException</code> is thrown.
	 * <p>
	 * 	If <code>in</code> is an unconnected piped input stream and <code>out</code> is an unconnected piped output stream, they may be connected by either the call: 
	 * 	<blockquote><pre>out.connect(in)</pre></blockquote>
	 * 	or the call: 
	 * 	<blockquote><pre>in.connect(out)</pre></blockquote>
	 * 	The two calls have the same effect.
	 * </p>
	 * 
	 * @param out The piped output stream to connect to.
	 * @exception IOException If an I/O error occurs.
	 */
	public void connect(PipedOutputStream out) throws IOException {
		out.connect(this);
	}

	/**
	 * Receives a <code>byte</code> of data.
	 * 
	 * @param b The <code>byte</code> to be written.
	 * @exception IOException If an I/O error occurs.
	 */
	synchronized void receive(int b) throws IOException {
		checkStateForReceive();
		++available;
		if (waiting != 0) {
			in = new ByteArrayInputStream(new byte[] { (byte) (b & 0xFF) });
			inSize = 1;
			this.notify();
		}
		else {
			if (out == null)
				out = new ByteArrayOutputStream();
			out.write(b);
		}
	}

	/**
	 * Receives data into an array of bytes.
	 * This method blocks until all the bytes are written to the buffer.
	 * 
	 * @param data The data.
	 * @param off The start offset in the data.
	 * @param len The number of bytes to write.
	 * @exception IOException If an I/O error occurs.
	 */
	synchronized void receive(byte[] data, int off, int len) throws IOException {
		checkStateForReceive();
		available += data.length;

		if (out == null)
			out = new ByteArrayOutputStream();
		out.write(data, off, len);
		if (waiting != 0)
			this.notify();
	}

	/**
	 * Check the state of the pipe
	 * 
	 * @throws IOException If the pipe is broken
	 */
	private synchronized void checkStateForReceive() throws IOException {
		if (!connected)
			throw new IOException("Pipe not connected");
		if (closedByWriter || closedByReader)
			throw new IOException("Pipe closed");
	}

	/**
	 * Reads the next byte of data from this piped input stream.
	 * The value byte is returned as an <code>int</code> in the range <code>0</code> to <code>255</code>.
	 * This method blocks until input data is available or an exception is thrown.
	 * 
	 * @return The next byte of data, or <code>-1</code> if the end of the stream is reached.
	 * @exception IOException If the pipe is {@link #connect(java.io.PipedOutputStream) unconnected}, 
	 * 					<a href=#BROKEN><code>broken</code></a>,
	 * 					closed, or if an I/O error occurs.
	 */
	public synchronized int read() throws IOException {
		prepareReading();
		if (inSize==0)
			return -1;
		--inSize;
		return in.read();
	}

	/**
	 * Reads up to <code>len</code> bytes of data from this piped input stream into an array of bytes.
	 * Less than <code>len</code> bytes will be read if the end of the data stream is reached or if <code>len</code> exceeds the pipe's buffer size.
	 * If <code>len</code> is zero, then no bytes are read and 0 is returned; otherwise, the method blocks until at least 1 byte of input is available or an exception is thrown.
	 * 
	 * @param data The buffer into which the data is read.
	 * @param off The start offset in the destination array <code>data</code>
	 * @param len The maximum number of bytes read.
	 * @return The total number of bytes read into the buffer, or <code>-1</code> if there is no more data because the end of the stream has been reached.
	 * 
	 * @exception NullPointerException If <code>data</code> is <code>null</code>.
	 * @exception IndexOutOfBoundsException If <code>off</code> is negative, <code>len</code> is negative, 
	 * 					or <code>len</code> is greater than <code>data.length - off</code>
	 * @exception IOException If the pipe is <a href=#BROKEN> <code>broken</code></a>, {@link #connect(java.io.PipedOutputStream) unconnected}, closed, or if an I/O error occurs.
	 */
	public synchronized int read(byte[] data, int off, int len) throws IOException {
		if (data == null)
			throw new NullPointerException();
		else if (off < 0 || len < 0 || len > data.length - off)
			throw new IndexOutOfBoundsException();
		else if (len == 0)
			return 0;

		prepareReading();
		int ret = in.read(data, off, len);
		inSize -= ret;
		return ret;
	}

	/**
	 * Check the reading state and prepare the input buffer
	 * 
	 * @throws IOException If an I/O error occurs.
	 */
	private synchronized void prepareReading() throws IOException {
		if (!connected)
			throw new IOException("Pipe not connected");
		else if (closedByReader && inSize==0)
			throw new IOException("Pipe closed");

		if (waiting != 0 || inSize == 0 && out == null) {
			++waiting;
			try {
				this.wait();
				--waiting;
			}
			catch (InterruptedException e) {
				--waiting;
				throw new IOException("Pipe closed", e);
			}
			if (!connected)
				throw new IOException("Pipe not connected");
			else if (closedByReader && inSize==0 && out==null)
				throw new IOException("Pipe closed");
		}
		if (inSize==0) {
			byte[] data = out!=null?out.toByteArray():new byte[0];
			inSize = data.length;
			in = new ByteArrayInputStream(data);
			out = null;
		}
	}

	/**
	 * Returns the number of bytes that can be read from this input stream without blocking.
	 * 
	 * @return The number of bytes that can be read from this input stream without blocking, 
	 * 				or {@code 0} if this input stream has been closed by invoking its {@link #close()} method, 
	 * 				or if the pipe is {@link #connect(java.io.PipedOutputStream) unconnected}, 
	 * 				or <a href=#BROKEN> <code>broken</code></a>.
	 * 
	 * @exception IOException If an I/O error occurs.
	 */
	public synchronized int available() throws IOException {
		return this.available;
	}

	/**
	 * Notifies all waiting threads that the last byte of data has been received.
	 */
	synchronized void receivedLast() {
		closedByWriter = true;
		notifyAll();
	}

	/**
	 * Closes this piped input stream and releases any system resources associated with the stream.
	 * 
	 * @exception IOException If an I/O error occurs.
	 */
	public void close() throws IOException {
		synchronized (this) {
			closedByReader = true;
		}
	}
}
