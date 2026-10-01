package com.kerosene.common.admin.cell;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "operations.cell")
public class CellOperationsProperties {
    public String bankUrl = "";
    public String cellId = "";
    public String networkId = "";
    public List<String> observers = List.of();
    public Map<String, String> signerKeys = Map.of();
    public Map<String, String> bankKeys = Map.of();
    public String targetReleaseDigest = "";
    public int minimumVotes = 1;
    public int minimumSignatures = 1;
    public long maximumAgeSeconds = 300;
    public long maximumBackupAgeSeconds = 86400;
    public long maximumRestoreAgeSeconds = 86400;
    public String keyStore = "";
    public String keyStorePassword = "";
    public String trustStore = "";
    public String trustStorePassword = "";
    public String planDirectory = "";
    public String kfeUrl = "";
    public String kfeAdminTokenFile = "";
    public void setBankUrl(String v) { bankUrl = v; }
    public void setCellId(String v) { cellId = v; }
    public void setNetworkId(String v) { networkId = v; }
    public void setObservers(List<String> v) { observers = List.copyOf(v); }
    public void setSignerKeys(Map<String, String> v) { signerKeys = Map.copyOf(v); }
    public void setBankKeys(Map<String, String> v) { bankKeys = Map.copyOf(v); }
    public void setTargetReleaseDigest(String v) { targetReleaseDigest = v; }
    public void setMinimumVotes(int v) { minimumVotes = v; }
    public void setMinimumSignatures(int v) { minimumSignatures = v; }
    public void setMaximumAgeSeconds(long v) { maximumAgeSeconds = v; }
    public void setMaximumBackupAgeSeconds(long v) { maximumBackupAgeSeconds = v; }
    public void setMaximumRestoreAgeSeconds(long v) { maximumRestoreAgeSeconds = v; }
    public void setKeyStore(String v) { keyStore = v; }
    public void setKeyStorePassword(String v) { keyStorePassword = v; }
    public void setTrustStore(String v) { trustStore = v; }
    public void setTrustStorePassword(String v) { trustStorePassword = v; }
    public void setPlanDirectory(String v) { planDirectory = v; }
    public void setKfeUrl(String v) { kfeUrl = v; }
    public void setKfeAdminTokenFile(String v) { kfeAdminTokenFile = v; }
}
