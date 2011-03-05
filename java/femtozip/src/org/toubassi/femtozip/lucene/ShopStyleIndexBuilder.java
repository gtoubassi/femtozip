/**
 *   Copyright 2011 Garrick Toubassi
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.toubassi.femtozip.lucene;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * http://api.shopstyle.com/action/apiSearch?pid=uid4884-xxx-xx&fts=red+dress&min=0&count=10
 * @author gtoubassi
 *
 */
public class ShopStyleIndexBuilder {

    protected String searchString;
    protected int numProducts;
    protected String apiKey;
    protected String indexPath;

    protected void usage() {
        System.out.println("usage: --search searchstring --apikey key --numProducts number indexpath");
        System.out.println("  eg.:  --search jeans --apikey uid4884-199-11 --numProducts 10000 /tmp/jeansindex");
        System.exit(1);
    }
    
    protected void parseArgs(String[] args) {
        for (int i = 0, count = args.length; i < count; i++) {
            String arg = args[i];
            
            if (arg.equals("--numProducts")) {
                numProducts = Integer.parseInt(args[++i]);
            }
            else if (arg.equals("--apiKey")) {
                apiKey = args[++i];
            }
            else if (arg.equals("--search")) {
                searchString = args[++i];
            }
            else {
                indexPath = arg;
            }
        }
        
        if (numProducts < 1 || searchString == null || indexPath == null || apiKey == null) {
            usage();
        }
    }
    
    protected Document createDocumentForProduct(JSONObject product) throws JSONException {
        Document doc = new Document();
        
        String name = product.getString("name");
        doc.add(new Field("name", name, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
        
        String price = product.getString("price");
        doc.add(new Field("price", price, Field.Store.YES, Field.Index.NOT_ANALYZED_NO_NORMS));
        
        String retailer = product.getString("retailer");
        doc.add(new Field("retailer", retailer, Field.Store.NO, Field.Index.ANALYZED_NO_NORMS));
        
        String retailerId = product.getString("retailerId");
        doc.add(new Field("retailerId", retailerId, Field.Store.YES, Field.Index.NO));
        
        String description = product.getString("description");
        doc.add(new Field("description", description, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
        
        if (product.has("brandName")) {
            String brandName = product.getString("brandName");
            doc.add(new Field("brandName", brandName, Field.Store.NO, Field.Index.ANALYZED_NO_NORMS));
        }
        
        if (product.has("brandId")) {
            String brandId = product.getString("brandId");
            doc.add(new Field("brandId", brandId, Field.Store.YES, Field.Index.NO));
        }
        
        String url = product.getString("url");
        doc.add(new Field("url", url, Field.Store.YES, Field.Index.NO));
        
        JSONArray images = product.getJSONArray("images");
        if (images != null && images.length() > 0) {
            //http://resources.shopstyle.com/sim/f3/5c/f35c717afe4351424625897e57504f51_medium/blahblah
            JSONObject image = images.getJSONObject(0);
            String imageUrl = image.getString("url");
            String[] parts = imageUrl.split("/");
            if (parts.length > 6) {
                parts = parts[6].split("_");
                String hash = parts[0];
                
                doc.add(new Field("image", hash, Field.Store.YES, Field.Index.NO));
            }
        }
        
        if (product.has("colors")) {
            JSONArray colors = product.getJSONArray("colors");
            if (colors != null && colors.length() > 0) {
                for (int i = 0; i < colors.length(); i++) {
                    JSONObject color = colors.getJSONObject(i);
                    String colorName = color.getString("name");
                    if (colorName != null) {
                        doc.add(new Field("color", colorName, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
                    }
                }
            }
        }
        
        if (product.has("sizes")) {
            JSONArray sizes = product.getJSONArray("sizes");
            if (sizes != null && sizes.length() > 0) {
                for (int i = 0; i < sizes.length(); i++) {
                    JSONObject size = sizes.getJSONObject(i);
                    String sizeName = size.getString("name");
                    if (sizeName != null) {
                        doc.add(new Field("size", sizeName, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
                    }
                }
            }
        }
        
        if (product.has("categories")) {
            JSONArray categories = product.getJSONArray("categories");
            if (categories != null && categories.length() > 0) {
                for (int i = 0; i < categories.length(); i++) {
                    String categoryName = categories.getString(i);
                    if (categoryName != null) {
                        doc.add(new Field("category", categoryName, Field.Store.YES, Field.Index.ANALYZED_NO_NORMS));
                    }
                }
            }
        }
        
        return doc;
    }
    
    protected void run(String[] args) throws IOException, JSONException {
        parseArgs(args);
        
        String baseUrl = "http://api.shopstyle.com/action/apiSearch?format=json2&pid=" + apiKey + "&fts=" + URLEncoder.encode(searchString, "UTF-8") + "&count=100";
        
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriter indexWriter = new IndexWriter(new File(indexPath), analyzer, true, MaxFieldLength.UNLIMITED);
        
        HttpClient httpclient = new DefaultHttpClient();
        
        for (int i = 0; i < numProducts; i+= 100) {
            String url = baseUrl + "&min=" + i;
            System.out.println(url);

            HttpGet httpGet = new HttpGet(url);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = httpclient.execute(httpGet, responseHandler);
            JSONObject results = new JSONObject(responseBody);
            JSONArray products = results.getJSONArray("products");
            for (int j = 0; j < products.length(); j++) {
                JSONObject product = products.getJSONObject(j);
                
                Document document = createDocumentForProduct(product);
                indexWriter.addDocument(document);
            }
        }
        
        httpclient.getConnectionManager().shutdown();
        
        
        indexWriter.optimize(1, true);
        indexWriter.close();
    }
    
    public static void main(String[] args) throws IOException, JSONException {
        ShopStyleIndexBuilder builder = new ShopStyleIndexBuilder();
        builder.run(args);
    }
}
