package com.glmx.tools.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glmx.tools.common.BusinessException;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class StressTestService {

    private final OkHttpClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public StressTestService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    public StressTestResult runTest(StressTestRequest request) {
        if (request.getUrl() == null || request.getUrl().isEmpty()) {
            throw new BusinessException("URL不能为空");
        }

        int threads = request.getThreads() != null ? request.getThreads() : 1;
        int requestsPerThread = request.getRequestsPerThread() != null ? request.getRequestsPerThread() : 10;
        int totalRequests = threads * requestsPerThread;

        if (threads > 100) {
            throw new BusinessException("并发线程数不能超过100");
        }
        if (totalRequests > 10000) {
            throw new BusinessException("总请求数不能超过10000");
        }

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);
        AtomicLong totalResponseTime = new AtomicLong(0);
        AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
        AtomicLong maxResponseTime = new AtomicLong(0);
        ConcurrentHashMap<Integer, AtomicInteger> statusCodes = new ConcurrentHashMap<>();
        List<Long> responseTimes = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch readyLatch = new CountDownLatch(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(totalRequests);

        AtomicLong firstRequestTime = new AtomicLong(0);
        AtomicLong lastRequestTime = new AtomicLong(0);

        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                try {
                    Request.Builder requestBuilder = new Request.Builder().url(request.getUrl());

                    String method = request.getMethod() != null ? request.getMethod().toUpperCase() : "GET";
                    
                    if (request.getHeaders() != null) {
                        for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                            requestBuilder.addHeader(header.getKey(), header.getValue());
                        }
                    }

                    switch (method) {
                        case "POST":
                            requestBuilder.post(createRequestBody(request));
                            break;
                        case "PUT":
                            requestBuilder.put(createRequestBody(request));
                            break;
                        case "DELETE":
                            if (request.getBody() != null && !request.getBody().isEmpty()) {
                                requestBuilder.delete(createRequestBody(request));
                            } else {
                                requestBuilder.delete();
                            }
                            break;
                        default:
                            requestBuilder.get();
                    }

                    Request builtRequest = requestBuilder.build();

                    readyLatch.countDown();
                    startLatch.await();

                    for (int i = 0; i < requestsPerThread; i++) {
                        long requestStart = System.nanoTime();
                        
                        firstRequestTime.compareAndSet(0, System.currentTimeMillis());

                        try {
                            try (Response response = client.newCall(builtRequest).execute()) {
                                long responseTime = (System.nanoTime() - requestStart) / 1_000_000;
                                responseTimes.add(responseTime);
                                
                                totalResponseTime.addAndGet(responseTime);
                                minResponseTime.updateAndGet(current -> Math.min(current, responseTime));
                                maxResponseTime.updateAndGet(current -> Math.max(current, responseTime));
                                
                                lastRequestTime.set(System.currentTimeMillis());
                                
                                int statusCode = response.code();
                                statusCodes.computeIfAbsent(statusCode, k -> new AtomicInteger(0)).incrementAndGet();
                                
                                if (response.isSuccessful()) {
                                    successCount.incrementAndGet();
                                } else {
                                    failureCount.incrementAndGet();
                                }
                            }
                        } catch (Exception e) {
                            failureCount.incrementAndGet();
                            statusCodes.computeIfAbsent(0, k -> new AtomicInteger(0)).incrementAndGet();
                        } finally {
                            finishLatch.countDown();
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        try {
            readyLatch.await();
            long startTime = System.currentTimeMillis();
            startLatch.countDown();
            
            finishLatch.await(5, TimeUnit.MINUTES);
            
            long endTime = lastRequestTime.get() > 0 ? lastRequestTime.get() : System.currentTimeMillis();
            long totalTime = endTime - startTime;

            executor.shutdown();

            StressTestResult result = new StressTestResult();
            result.setTotalRequests(totalRequests);
            result.setSuccessCount(successCount.get());
            result.setFailureCount(failureCount.get());
            result.setTotalTime(totalTime);
            
            int completedRequests = successCount.get() + failureCount.get();
            if (completedRequests > 0 && totalTime > 0) {
                result.setRequestsPerSecond(completedRequests * 1000.0 / totalTime);
            } else {
                result.setRequestsPerSecond(0.0);
            }
            
            int responseCount = responseTimes.size();
            if (responseCount > 0) {
                result.setAverageResponseTime(totalResponseTime.get() / (double) responseCount);
            } else {
                result.setAverageResponseTime(0.0);
            }
            
            result.setMinResponseTime(minResponseTime.get() == Long.MAX_VALUE ? 0 : minResponseTime.get());
            result.setMaxResponseTime(maxResponseTime.get());
            result.setStatusCodes(statusCodes);
            result.setPercentiles(calculatePercentiles(responseTimes));

            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdown();
            throw new BusinessException("压测被中断");
        }
    }

    private RequestBody createRequestBody(StressTestRequest request) {
        String body = request.getBody() != null ? request.getBody() : "";
        String contentType = request.getContentType() != null ? request.getContentType() : "application/json";
        MediaType mediaType = MediaType.parse(contentType);
        return RequestBody.create(body, mediaType);
    }

    private Map<String, Long> calculatePercentiles(List<Long> responseTimes) {
        if (responseTimes.isEmpty()) {
            return new HashMap<>();
        }

        List<Long> sorted = new ArrayList<>(responseTimes);
        sorted.sort(Long::compareTo);

        Map<String, Long> percentiles = new ConcurrentHashMap<>();
        percentiles.put("p50", calculatePercentile(sorted, 0.50));
        percentiles.put("p75", calculatePercentile(sorted, 0.75));
        percentiles.put("p90", calculatePercentile(sorted, 0.90));
        percentiles.put("p95", calculatePercentile(sorted, 0.95));
        percentiles.put("p99", calculatePercentile(sorted, 0.99));

        return percentiles;
    }

    private long calculatePercentile(List<Long> sorted, double percentile) {
        if (sorted.size() == 1) {
            return sorted.get(0);
        }
        
        double position = percentile * (sorted.size() - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        
        if (lower == upper) {
            return sorted.get(lower);
        }
        
        double weight = position - lower;
        return Math.round(sorted.get(lower) * (1 - weight) + sorted.get(upper) * weight);
    }

    public static class StressTestRequest {
        private String url;
        private String method;
        private Map<String, String> headers;
        private String body;
        private String contentType;
        private Integer threads;
        private Integer requestsPerThread;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }
        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public Integer getThreads() { return threads; }
        public void setThreads(Integer threads) { this.threads = threads; }
        public Integer getRequestsPerThread() { return requestsPerThread; }
        public void setRequestsPerThread(Integer requestsPerThread) { this.requestsPerThread = requestsPerThread; }
    }

    public static class StressTestResult {
        private Integer totalRequests;
        private Integer successCount;
        private Integer failureCount;
        private Long totalTime;
        private Double requestsPerSecond;
        private Double averageResponseTime;
        private Long minResponseTime;
        private Long maxResponseTime;
        private Map<Integer, AtomicInteger> statusCodes;
        private Map<String, Long> percentiles;

        public Integer getTotalRequests() { return totalRequests; }
        public void setTotalRequests(Integer totalRequests) { this.totalRequests = totalRequests; }
        public Integer getSuccessCount() { return successCount; }
        public void setSuccessCount(Integer successCount) { this.successCount = successCount; }
        public Integer getFailureCount() { return failureCount; }
        public void setFailureCount(Integer failureCount) { this.failureCount = failureCount; }
        public Long getTotalTime() { return totalTime; }
        public void setTotalTime(Long totalTime) { this.totalTime = totalTime; }
        public Double getRequestsPerSecond() { return requestsPerSecond; }
        public void setRequestsPerSecond(Double requestsPerSecond) { this.requestsPerSecond = requestsPerSecond; }
        public Double getAverageResponseTime() { return averageResponseTime; }
        public void setAverageResponseTime(Double averageResponseTime) { this.averageResponseTime = averageResponseTime; }
        public Long getMinResponseTime() { return minResponseTime; }
        public void setMinResponseTime(Long minResponseTime) { this.minResponseTime = minResponseTime; }
        public Long getMaxResponseTime() { return maxResponseTime; }
        public void setMaxResponseTime(Long maxResponseTime) { this.maxResponseTime = maxResponseTime; }
        public Map<Integer, AtomicInteger> getStatusCodes() { return statusCodes; }
        public void setStatusCodes(Map<Integer, AtomicInteger> statusCodes) { this.statusCodes = statusCodes; }
        public Map<String, Long> getPercentiles() { return percentiles; }
        public void setPercentiles(Map<String, Long> percentiles) { this.percentiles = percentiles; }
    }
}
