package com.kerosene.common.admin.cell;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import static org.junit.jupiter.api.Assertions.*;

class CellOperationsPropertiesTest {
    @Test void runtimeConfigurationBindsExpectedIdentitiesAndTrust() {
        var source = new MapConfigurationPropertySource(Map.of(
                "operations.cell.bank-url", "https://bank.example.invalid",
                "operations.cell.cell-id", "cell-test", "operations.cell.network-id", "testnet",
                "operations.cell.signer-keys.bank", "PUBLIC_KEY_BASE64",
                "operations.cell.observers", "bank-one,bank-two", "operations.cell.minimum-votes", "2",
                "operations.cell.maximum-backup-age-seconds", "3600",
                "operations.cell.kfe-url", "https://kfe.example.invalid"));
        var config = new Binder(source).bind("operations.cell", Bindable.of(CellOperationsProperties.class)).get();
        assertEquals("cell-test", config.cellId); assertEquals(2, config.minimumVotes); assertEquals(2, config.observers.size());
        assertEquals("PUBLIC_KEY_BASE64", config.signerKeys.get("bank")); assertEquals(3600, config.maximumBackupAgeSeconds);
        assertEquals("https://kfe.example.invalid", config.kfeUrl);
    }
}
