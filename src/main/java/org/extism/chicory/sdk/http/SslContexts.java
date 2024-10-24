
package org.extism.chicory.sdk.http;



import com.workoss.boot.util.StreamUtils;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author workoss
 */
@SuppressWarnings("ALL")
public class SslContexts {

	public static SslConfig parse(InputStream[] certificates, X509TrustManager x509TrustManager,
			InputStream pfxCertificate, char[] pfxPassword) {

		try {
			SslConfig sslConfig = new SslConfig();
			KeyManager[] keyManagers = prepareKeyManagers(pfxCertificate, pfxPassword);
			X509TrustManager trustManager;
			if (x509TrustManager != null) {
				// 有限用户自定义
				trustManager = x509TrustManager;
			}
			else {
				trustManager = prepareX509TrustManager(certificates);
				if (trustManager == null) {
					trustManager = unSafeTrucstManager;
				}
			}

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(keyManagers, new TrustManager[] { trustManager }, new SecureRandom());
			sslConfig.setSslContext(sslContext);
			sslConfig.setX509TrustManager(trustManager);

			return sslConfig;

		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	private static X509TrustManager unSafeTrucstManager = new X509TrustManager() {
		@Override
		public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

		}

		@Override
		public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}
	};

	private static X509TrustManager prepareX509TrustManager(InputStream... certificates)
			throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException {
		if (certificates == null || certificates.length == 0) {
			return null;
		}
		CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null);
		// 处理证书
		for (int i = 0, length = certificates.length; i < length; i++) {
			keyStore.setCertificateEntry(String.valueOf(i + 1),
					certificateFactory.generateCertificate(certificates[i]));
			StreamUtils.close(certificates[i]);

		}
		// to build X509
		TrustManagerFactory trustManagerFactory = TrustManagerFactory
				.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		// 用我们之前的keyStore实例初始化TrustManagerFactory,这样tmf就会信任keyStore中的证书
		trustManagerFactory.init(keyStore);
		TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
		if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
			throw new IllegalStateException("Unexpected default trust managers:"
					+ Arrays.stream(trustManagers).map(Objects::toString).collect(Collectors.joining(",")));
		}
		return (X509TrustManager) trustManagers[0];
	}

	private static KeyManager[] prepareKeyManagers(InputStream pfxCertificate, char[] pfxPassword) throws Exception {
		if (pfxCertificate == null) {
			return null;
		}
		if (!(pfxPassword != null && pfxPassword.length > 0)) {
			return null;
		}
		KeyStore clientKS = KeyStore.getInstance("PKCS12");
		clientKS.load(pfxCertificate, pfxPassword);
		KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
		factory.init(clientKS, pfxPassword);
		return factory.getKeyManagers();
	}

	public static final class SslConfig {

		private X509TrustManager x509TrustManager;

		private SSLContext sslContext;

		public SSLSocketFactory getSslSocketFactory() {
			return sslContext.getSocketFactory();
		}

		public X509TrustManager getX509TrustManager() {
			return x509TrustManager;
		}

		public void setX509TrustManager(X509TrustManager x509TrustManager) {
			this.x509TrustManager = x509TrustManager;
		}

		public SSLContext getSslContext() {
			return sslContext;
		}

		public void setSslContext(SSLContext sslContext) {
			this.sslContext = sslContext;
		}

	}

}
