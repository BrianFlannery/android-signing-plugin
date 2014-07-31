package org.jenkinsci.plugins.androidsigning.impl;

import com.cloudbees.plugins.credentials.CredentialsDescriptor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import hudson.Extension;
import hudson.util.IOException2;
import hudson.util.Secret;
import jenkins.security.CryptoConfidentialKey;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItem;
import org.jenkinsci.plugins.androidsigning.KeystoreCredentials;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.*;
import java.security.GeneralSecurityException;


public class KeystoreCredentialsImpl extends BaseStandardCredentials implements KeystoreCredentials{
    private static final CryptoConfidentialKey KEY = new CryptoConfidentialKey(KeystoreCredentialsImpl.class.getName());

    private final @Nonnull Secret password;
    private final @Nonnull String fileName;
    private final @Nonnull byte[] data;

    @DataBoundConstructor
    public KeystoreCredentialsImpl(@CheckForNull CredentialsScope scope, @CheckForNull String id, @CheckForNull String description, @Nonnull FileItem file, @CheckForNull String fileName, @CheckForNull String data, @CheckForNull String password) throws IOException {
        super(scope, id, description);
        String name = file.getName();
        if (name.length() > 0) {
            this.fileName = name.replaceFirst("^.+[/\\\\]", "");
            byte[] unencrypted = file.get();
            try {
                this.data = KEY.encrypt().doFinal(unencrypted);
            } catch (GeneralSecurityException x) {
                throw new IOException2(x);
            }
        } else {
            this.fileName = fileName;
            this.data = Base64.decodeBase64(data);
        }
        this.password = Secret.fromString(password);
    }

    public String getTempPath() throws IOException {
        File tmp = File.createTempFile("keystore", null);
        FileOutputStream out = new FileOutputStream(tmp);
        out.write(unencrypted());
        out.close();
        tmp.deleteOnExit();
        return tmp.getAbsolutePath();
    }

    public String getFileName() {
        return fileName;
    }

    public InputStream getContent() throws IOException {
        return new ByteArrayInputStream(unencrypted());
    }

    public String getPassphrase() {
        return password.getPlainText();
    }

    private byte[] unencrypted() throws IOException {
        try {
            return KEY.decrypt().doFinal(data);
        } catch (GeneralSecurityException x) {
            throw new IOException2(x);
        }
    }

    @Extension
    public static class DescriptorImpl extends CredentialsDescriptor {

        @Override public String getDisplayName() {
            return Messages.KeystoreCredentialsImpl_keystore();
        }

    }
}