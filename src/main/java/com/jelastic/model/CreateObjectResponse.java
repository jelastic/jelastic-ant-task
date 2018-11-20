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

package com.jelastic.model;

public class CreateObjectResponse extends Response {
    private Response response;
    private String source;
    private DebugResponse debug;

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public DebugResponse getDebug() {
        return debug;
    }

    public void setDebug(DebugResponse debug) {
        this.debug = debug;
    }

    public static class Response extends com.jelastic.model.Response {
        private int id;
        private String source;
        private ObjectResponse object;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public ObjectResponse getObject() {
            return object;
        }

        public void setObject(ObjectResponse object) {
            this.object = object;
        }
    }

    public static class ObjectResponse {
        private int id;
        private int developer;
        private Long uploadDate;

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getDeveloper() {
            return developer;
        }

        public void setDeveloper(int developer) {
            this.developer = developer;
        }

        public Long getUploadDate() {
            return uploadDate;
        }

        public void setUploadDate(Long uploadDate) {
            this.uploadDate = uploadDate;
        }
    }
}
