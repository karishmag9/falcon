/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.falcon.regression.core.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.falcon.regression.core.bundle.Bundle;
import org.apache.falcon.regression.core.helpers.ColoHelper;
import org.apache.falcon.regression.core.response.ServiceResponse;
import org.apache.hadoop.security.authentication.client.AuthenticationException;
import org.apache.log4j.Logger;
import org.testng.Assert;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * util methods related to bundle.
 */
public final class BundleUtil {
    private BundleUtil() {
        throw new AssertionError("Instantiating utility class...");
    }
    private static final Logger LOGGER = Logger.getLogger(BundleUtil.class);

    public static Bundle readLateDataBundle(String appPath, String testName) throws IOException {
        return generateBundleFromTemplate("LateDataBundles", appPath, testName);
    }

    public static Bundle readRetryBundle(String appPath, String testName) throws IOException {
        return generateBundleFromTemplate("RetryTests", appPath, testName);
    }

    public static Bundle readRetentionBundle(String appPath, String testName) throws IOException {
        return generateBundleFromTemplate("RetentionBundles", appPath, testName);
    }

    public static Bundle readELBundle(String appPath, String testName) throws IOException {
        return generateBundleFromTemplate("ELbundle", appPath, testName);
    }

    public static Bundle readHCatBundle(String appPath, String testName) throws IOException {
        return generateBundleFromTemplate("hcat", appPath, testName);
    }

    public static Bundle readHCat2Bundle(String appPath, String testName) throws IOException {
        return generateBundleFromTemplate("hcat_2", appPath, testName);
    }

    public static Bundle readLocalDCBundle(String appPath, String testName) throws IOException {
        return generateBundleFromTemplate("LocalDC_feedReplicaltion_BillingRC", appPath, testName);
    }

    public static Bundle readUpdateBundle(String appPath, String testName) throws IOException {
        return generateBundleFromTemplate("updateBundle", appPath, testName);
    }

    /** Generate a bundle from the template stored in a directory
     * @param templatePath name of directory where template is stored
     * @param appPath application path where staging/temp/working directory will live
     * @param testName name of test
     * @return the customized bundle
     * @throws IOException
     */
    private static Bundle generateBundleFromTemplate(String templatePath, String appPath,
            String testName) throws IOException {
        final Bundle bundle = readBundleFromFolder(templatePath);
        bundle.updateClusterLocations(HadoopUtil.stitchHdfsPath(appPath, testName));
        return bundle;
    }
    
    private static Bundle readBundleFromFolder(final String folderPath) throws IOException {
        LOGGER.info("Loading xmls from directory: " + folderPath);
        File directory = null;
        try {
            directory = new File(BundleUtil.class.getResource("/" + folderPath).toURI());
        } catch (URISyntaxException e) {
            Assert.fail("could not find dir: " + folderPath);
        }
        final Collection<File> list = FileUtils.listFiles(directory, new String[] {"xml"}, true);
        File[] files = list.toArray(new File[list.size()]);
        Arrays.sort(files);
        String clusterData = "";
        final List<String> dataSets = new ArrayList<String>();
        String processData = "";

        for (File file : files) {
            LOGGER.info("Loading data from path: " + file.getAbsolutePath());
            final String data = IOUtils.toString(file.toURI());

            if (data.contains("uri:ivory:cluster:0.1") || data.contains("uri:falcon:cluster:0.1")) {
                LOGGER.info("data been added to cluster");
                clusterData = data;
            } else if (data.contains("uri:ivory:feed:0.1")
                    ||
                data.contains("uri:falcon:feed:0.1")) {
                LOGGER.info("data been added to feed");
                dataSets.add(InstanceUtil.setFeedACL(data));
            } else if (data.contains("uri:ivory:process:0.1")
                    ||
                data.contains("uri:falcon:process:0.1")) {
                LOGGER.info("data been added to process");
                processData = data;
            }
        }
        Assert.assertNotNull(clusterData, "expecting cluster data to be non-empty");
        Assert.assertTrue(!dataSets.isEmpty(), "expecting feed data to be non-empty");
        return new Bundle(clusterData, dataSets, processData);
    }

    public static void submitAllClusters(ColoHelper prismHelper, Bundle... b)
        throws IOException, URISyntaxException, AuthenticationException {
        for (Bundle aB : b) {
            ServiceResponse r = prismHelper.getClusterHelper().submitEntity(aB.getClusters().get(0));
            Assert.assertTrue(r.getMessage().contains("SUCCEEDED"));

        }
    }

}