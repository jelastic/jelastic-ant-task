/*
 * Copyright 2012 Jelastic.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jelastic;

import com.jelastic.model.AuthenticationResponse;
import com.jelastic.model.CreateObjectResponse;
import com.jelastic.model.DeployResponse;
import com.jelastic.model.UploadResponse;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class Jelastic extends Task {
    private String email;
    private String password;
    private String context;
    private String environment;
    private String apihoster;

    private String dir;
    private String fileName;


    public String getApihoster() {
        return apihoster;
    }

    public void setApihoster(String apihoster) {
        this.apihoster = apihoster;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getDir() {
        return dir;
    }

    public String getFileName() {
        return fileName;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getContext() {
        return context;
    }

    public String getEnvironment() {
        return environment;
    }

    @Override
    public void execute() throws BuildException {
        JelasticService jelasticService = new JelasticService(getProject());
        jelasticService.setDir(getDir());
        jelasticService.setFilename(getFileName());
        jelasticService.setApiHoster(getApihoster());
        jelasticService.setEnvironment(getEnvironment());
        jelasticService.setContext(getContext());

        AuthenticationResponse authenticationResponse = jelasticService.authentication(getEmail(), getPassword());
        if (authenticationResponse.isNotOK()) {
            log("Authentication : FAILED");
            log("         Error : " + authenticationResponse.getError());

            return;
        }

        log("------------------------------------------------------------------------");
        log("   Authentication : SUCCESS");
        log("          Session : " + authenticationResponse.getSession());
        log("              Uid : " + authenticationResponse.getUid());
        log("------------------------------------------------------------------------");

        UploadResponse uploadResponse = jelasticService.upload(authenticationResponse);
        if (uploadResponse.isNotOK()) {
            log("File upload : FAILED");
            log("      Error : " + uploadResponse.getError());
            return;
        }

        log("      File UpLoad : SUCCESS");
        log("         File URL : " + uploadResponse.getFile());
        log("        File size : " + uploadResponse.getSize());
        log("------------------------------------------------------------------------");

        CreateObjectResponse createObject = jelasticService.createObject(uploadResponse, authenticationResponse);
        if (createObject.isNotOK()) {
            return;
        }

        log("File registration : SUCCESS");
        log("  Registration ID : " + createObject.getResponse().getObject().getId());
        log("     Developer ID : " + createObject.getResponse().getObject().getDeveloper());
        log("------------------------------------------------------------------------");

        DeployResponse deploy = jelasticService.deploy(authenticationResponse, uploadResponse);
        if (deploy.getResponse().isNotOK()) {
            log("          Deploy : FAILED");
            log("           Error : " + deploy.getResponse().getError());
            return;
        }

        log("      Deploy file : SUCCESS");
        log("       Deploy log :");
        log(deploy.getResponse().getResponses()[0].getOut());
    }
}

