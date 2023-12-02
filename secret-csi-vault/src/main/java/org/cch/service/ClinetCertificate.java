package org.cch.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import org.cch.config.CertificateConfig;
import org.cch.dto.ClientDTO;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ClinetCertificate {

    @Inject
    CertificateConfig certificateConfig;

    @Inject
    Logger log;

    public static final String CERTIFICATE_TYPE = "X509";

    private boolean validateCertificate(X509Certificate parent, X509Certificate child)  {

        try {
            child.verify(parent.getPublicKey());
        } catch (GeneralSecurityException e) {
            log.error(e.getMessage());
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private Certificate getCertificate(InputStream inputStream) throws CertificateException, IOException {
        try (inputStream) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance(CERTIFICATE_TYPE);
            return certificateFactory.generateCertificate(inputStream);
        }
    }


    public boolean validateCertificate(ClientDTO clientDTO) {
        X509Certificate client = null;
        
        try {
            client = (X509Certificate) this.getCertificate(new FileInputStream(new File(clientDTO.clientCertificate().uploadedFile().toUri())));
            var chain = (X509Certificate)this.getCertificate(new FileInputStream(certificateConfig.certificateChainPath().orElseThrow()));
            if (!this.validateCertificate(chain, client)) {
                log.warn("client validate fail.");
                return false;
            }
            return true;
        } catch (CertificateException e) {
           log.error(e.getMessage());
        } catch (FileNotFoundException e) {
            log.error("FILE NOT FOUND.");
            log.error(e.getMessage());
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        
        return false;

    }



}
