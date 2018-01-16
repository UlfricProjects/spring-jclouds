package com.ulfric.spring.jclouds.blobstore.impl;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.io.Payload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.ulfric.spring.jclouds.blobstore.BlobRepository;

@Repository
public class BlobRepositoryImpl implements BlobRepository {

	@Inject
	private ObjectMapper json;

	@Value("${BUCKET_NAME:springjclouds}")
	private String containerName;

	@Value("${JCLOUDS_PROVIDER:transient}")
	private String jcloudsProvider;

	@Value("${JCLOUDS_ID:none}")
	private String jcloudsId;

	@Value("${JCLOUDS_SECRET:}")
	private String jcloudsSecret;

	private BlobStoreContext context;

	@PostConstruct
	public void createBucket() {
		context = ContextBuilder.newBuilder(jcloudsProvider)
				.credentials(jcloudsId, jcloudsSecret.replace("\\n", "\n"))
				.build(BlobStoreContext.class);

		getBlobStore().createContainerInLocation(null, containerName);
	}

	@Override
	public void put(String key, Object value) {
		BlobStore blobStore = getBlobStore();
		Blob blob;
		try {
			blob = blobStore.blobBuilder(key).payload(json.writeValueAsString(value)).build();
		} catch (JsonProcessingException exception) {
			throw new RuntimeException("Could not parse json", exception);
		}
		blobStore.putBlob(containerName, blob);
	}

	@Override
	public <T> T get(String key, Class<T> type) {
		Blob blob = getBlobStore().getBlob(containerName, key);
		if (blob == null) {
			return null;
		}

		Payload payload = blob.getPayload();
		try {
			return payload == null ? null : json.readValue(payload.openStream(), type);
		} catch (JsonSyntaxException | JsonIOException | IOException exception) {
			exception.printStackTrace(); // TODO proper error handling
			return null;
		}
	}

	public BlobStore getBlobStore() {
		return context.getBlobStore();
	}

}
