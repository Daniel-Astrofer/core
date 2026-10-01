package com.kerosene.common.release.bank;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "release.bank-observer")
public class BankReleaseProperties {
    public boolean enabled;
    public String observerId = "";
    public String networkId = "";
    public String targetDirectory = "";
    public String privateKeyFile = "";
    public String publicKeyDerBase64 = "";
    public List<String> clientSpkiDigests = List.of();
    public void setEnabled(boolean v) { enabled = v; }
    public void setObserverId(String v) { observerId = v; }
    public void setNetworkId(String v) { networkId = v; }
    public void setTargetDirectory(String v) { targetDirectory = v; }
    public void setPrivateKeyFile(String v) { privateKeyFile = v; }
    public void setPublicKeyDerBase64(String v) { publicKeyDerBase64 = v; }
    public void setClientSpkiDigests(List<String> v) { clientSpkiDigests = List.copyOf(v); }
}
