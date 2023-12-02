package org.cch.config;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "pki")
public interface CertificateConfig {

    Optional<String> certificateChainPath();

}
