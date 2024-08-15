import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;

import java.util.concurrent.TimeUnit;

public class TokenService {

    private final Cache cache;
    private final TokenProvider tokenProvider;
    private final HazelcastInstance hazelcastInstance;

    public TokenService(Cache cache, TokenProvider tokenProvider) {
        this.cache = cache;
        this.tokenProvider = tokenProvider;
        this.hazelcastInstance = Hazelcast.newHazelcastInstance();
    }

    public String getToken() {
        String token = cache.get("authToken");

        if (token == null) {
            ILock lock = hazelcastInstance.getLock("tokenLock");

            try {
                // Kilidi belirli bir süre için al
                if (lock.tryLock(10, TimeUnit.SECONDS)) {
                    try {
                        // Cache tekrar kontrol et
                        token = cache.get("authToken");
                        if (token == null) {
                            // Yeni token al
                            token = tokenProvider.requestNewToken();
                            cache.put("authToken", token);
                        }
                    } finally {
                        // Kilidi serbest bırak
                        lock.unlock();
                    }
                } else {
                    // Kilit alınamazsa, token'ı cache'ten yeniden dene
                    token = cache.get("authToken");
                }
            } catch (InterruptedException e) {
                // Hata durumunda loglama yapılabilir
                Thread.currentThread().interrupt();
            }
        }

        return token;
    }
}
