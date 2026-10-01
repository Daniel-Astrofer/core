package com.kerosene.common.admin.cell;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BankObservationClientTest {
    @Test void refusesUntrustedOriginsAndMissingMtlsBeforeNetwork() {
        var config = new CellOperationsProperties(); var client = new BankObservationClient(config);
        for (String origin : new String[]{"http://localhost", "https://user:secret@example.invalid", "https://example.invalid/path", "https://example.invalid?query"}) {
            config.bankUrl = origin; assertThrows(Exception.class, client::fetch);
        }
        config.bankUrl = "https://example.invalid"; assertThrows(Exception.class, client::fetch);
    }
}
