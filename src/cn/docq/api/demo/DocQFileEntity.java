package cn.docq.api.demo;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Random;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HTTP;

import android.os.ParcelFileDescriptor;

public class DocQFileEntity implements HttpEntity {
	private final static char[] MULTIPART_CHARS = "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
			.toCharArray();

	private static String generateBoundary() {
		final StringBuilder buffer = new StringBuilder();
		final Random rand = new Random();
		final int count = rand.nextInt(11) + 30; // a random size from 30 to 40
		for (int i = 0; i < count; i++) {
			buffer.append(MULTIPART_CHARS[rand.nextInt(MULTIPART_CHARS.length)]);
		}
		return buffer.toString();
	}

	private String fileName;
	private ParcelFileDescriptor file;
	private List<NameValuePair> params;
	private String charset;
	private String boundary;

	private long contentLength;

	public DocQFileEntity(String fileName, ParcelFileDescriptor file,
			List<NameValuePair> params) {
		this.fileName = fileName;
		this.file = file;
		this.params = params;
		this.charset = "UTF-8";
		this.boundary = DocQFileEntity.generateBoundary();
		this.contentLength = this.computeContentLength();
	}

	private long computeContentLength() {
		try {
			CounterOutputStream out = new CounterOutputStream();
			this.doWriteTo(out, false);
			out.close();
			return out.getLength() + this.file.getStatSize();
		} catch (IOException e) {
			e.printStackTrace();
			return -1;
		}
	}

	private void doWriteTo(OutputStream out, boolean writeFile)
			throws IOException {
		// 写请求参数
		StringBuilder data = new StringBuilder();
		for (NameValuePair param : this.params) {
			data.append("--").append(this.boundary).append("\r\n");
			data.append("Content-Disposition: form-data; name=\"")
					.append(param.getName()).append("\"\r\n");
			data.append("Content-Type: text/plain; charset=")
					.append(this.charset).append("\r\n");
			data.append("\r\n");
			data.append(param.getValue());
			data.append("\r\n");
		}
		out.write(data.toString().getBytes(this.charset));
		// 写文件
		data.setLength(0);
		data.append("--").append(this.boundary).append("\r\n");
		data.append(
				"Content-Disposition: form-data; name=\"file\"; filename=\"")
				.append(this.fileName).append("\"\r\n");
		data.append("Content-Type: application/octet-stream\r\n");
		data.append("\r\n");
		out.write(data.toString().getBytes(this.charset));
		if (writeFile) {
			byte[] buffer = new byte[1024];
			int readed = -1;
			FileInputStream fis = new FileInputStream(
					this.file.getFileDescriptor());
			while ((readed = fis.read(buffer)) != -1) {
				out.write(buffer, 0, readed);
			}
			fis.close();
		}
		data.setLength(0);
		data.append("\r\n");
		data.append("--").append(this.boundary).append("--\r\n");
		out.write(data.toString().getBytes(this.charset));
	}

	private static class CounterOutputStream extends OutputStream {
		private long length;

		public CounterOutputStream() {
		}

		public long getLength() {
			return this.length;
		}

		@Override
		public void write(int oneByte) throws IOException {
			this.length++;
		}

		@Override
		public void write(byte[] buffer) throws IOException {
			this.length += buffer.length;
		}

		@Override
		public void write(byte[] buffer, int offset, int count)
				throws IOException {
			this.length += count;
		}
	}

	@Override
	public void consumeContent() throws IOException {
		if (isStreaming()) {
			throw new UnsupportedOperationException(
					"Streaming entity does not implement #consumeContent()");
		}
	}

	@Override
	public InputStream getContent() throws IOException, IllegalStateException {
		throw new UnsupportedOperationException(
				"DocQ entity does not implement #getContent()");
	}

	public long getContentLength() {
		return this.contentLength;
	}

	@Override
	public Header getContentEncoding() {
		return null;
	}

	@Override
	public Header getContentType() {
		StringBuilder sb = new StringBuilder();
		sb.append("multipart/form-data; boundary=");
		sb.append(this.boundary);
		sb.append("; charset=");
		sb.append(this.charset);
		return new BasicHeader(HTTP.CONTENT_TYPE, sb.toString());
	}

	@Override
	public boolean isChunked() {
		return !isRepeatable();
	}

	@Override
	public boolean isRepeatable() {
		return this.contentLength != -1;
	}

	@Override
	public boolean isStreaming() {
		return !isRepeatable();
	}

	@Override
	public void writeTo(OutputStream outstream) throws IOException {
		this.doWriteTo(outstream, true);
	}
}
