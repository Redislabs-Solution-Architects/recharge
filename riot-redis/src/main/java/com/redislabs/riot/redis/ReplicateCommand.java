package com.redislabs.riot.redis;

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.redis.RedisDumpItemReader;
import org.springframework.batch.item.redis.RedisDumpItemWriter;
import org.springframework.batch.item.redis.support.KeyItemReader;
import org.springframework.batch.item.redis.support.KeyValue;
import org.springframework.batch.item.redis.support.LiveKeyItemReader;
import org.springframework.batch.item.redis.support.LiveKeyItemReader.LiveKeyItemReaderBuilder;

import com.redislabs.riot.AbstractTransferCommand;
import com.redislabs.riot.RedisConnectionOptions;
import com.redislabs.riot.RedisExportOptions;
import com.redislabs.riot.Transfer;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

@Command(name = "replicate", aliases = {
	"r" }, description = "Replicate a source Redis database in a target Redis database")
public class ReplicateCommand extends AbstractTransferCommand<KeyValue<String, byte[]>, KeyValue<String, byte[]>> {

    @Mixin
    private RedisConnectionOptions targetRedis = new RedisConnectionOptions();
    @Mixin
    private RedisExportOptions options = new RedisExportOptions();
    @Option(names = "--live", description = "Enable live replication")
    private boolean live;
    @Option(names = "--notification-queue", description = "Capacity of the keyspace notification queue (default: ${DEFAULT-VALUE})", paramLabel = "<int>", hidden = true)
    private int notificationQueueCapacity = LiveKeyItemReaderBuilder.DEFAULT_QUEUE_CAPACITY;
    @Option(names = "--flush-interval", description = "Duration between notification queue flushes (default: ${DEFAULT-VALUE})", paramLabel = "<ms>")
    private long flushPeriod = 50;

    @Override
    protected String taskName() {
	return "Replicating from";
    }

    @Override
    protected List<ItemReader<KeyValue<String, byte[]>>> readers() throws Exception {
	List<ItemReader<KeyValue<String, byte[]>>> readers = new ArrayList<>();
	readers.add(reader(
		configure(KeyItemReader.builder().scanCount(options.getScanCount()).scanMatch(options.getScanMatch()))
			.build()));
	if (live) {
	    readers.add(reader(configure(LiveKeyItemReader.builder().scanMatch(options.getScanMatch())
		    .queueCapacity(notificationQueueCapacity)).build()));
	}
	return readers;
    }

    private RedisDumpItemReader<String, String> reader(ItemReader<String> keyReader) throws Exception {
	RedisDumpItemReader<String, String> reader = configure(
		RedisDumpItemReader.builder().keyReader(keyReader).batch(options.getReaderBatchSize())
			.threads(options.getReaderThreads()).queueCapacity(options.getQueueCapacity())).build();
	reader.setName(toString(redisURI()));
	return reader;
    }

    @Override
    protected RedisDumpItemWriter<String, String> writer() throws Exception {
	RedisDumpItemWriter<String, String> writer = configure(RedisDumpItemWriter.builder().replace(true), targetRedis)
		.build();
	writer.setName(toString(targetRedis.redisURI()));
	return writer;
    }

    @Override
    protected ItemProcessor<KeyValue<String, byte[]>, KeyValue<String, byte[]>> processor() {
	return null;
    }

    @Override
    public List<Transfer<KeyValue<String, byte[]>, KeyValue<String, byte[]>>> transfers() throws Exception {
	List<Transfer<KeyValue<String, byte[]>, KeyValue<String, byte[]>>> transfers = super.transfers();
	if (live) {
	    transfers.get(1).setFlushPeriod(flushPeriod);
	}
	return transfers;
    }

}
