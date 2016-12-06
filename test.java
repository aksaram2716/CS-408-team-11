import java.io.*;
import java.sql.*;
import java.util.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.jsoup.Connection.*;

public class Crawler {
    static int nextURLID;
    static int nextURLIDScanned;
    public Properties props;
    Connection connection;
    int urlID;
    int maxURL;
    String root;
    String domain;
    String curr;

    Crawler() {
        nextURLID = 0;
        nextURLIDScanned = 0;

        urlID = 0;
        maxURL = 1000000;
        root = null;
        domain = null;
        curr = null;
    }

    public static void main(String[] args) {
        Crawler crawler = new Crawler();
        try {
            crawler.readProperties();
            crawler.createDB();
            crawler.openConnection();

            // loop as long as we have more URLS in the DB
            while (nextURLID < nextURLIDScanned) {
                if (crawler.curr != null) {
                    System.out.println("(next: " + nextURLID + " scanned: " + nextURLIDScanned + ") " + "id: " + crawler.urlID + " " + crawler.curr);
                    crawler.fetchURL(crawler.curr);
                }
                crawler.grabURLInDB(nextURLID);
                nextURLID++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readProperties() throws IOException {
        props = new Properties();
        FileInputStream in = new FileInputStream("database.properties");
        props.load(in);
        this.maxURL = Integer.parseInt(props.getProperty("crawler.maxurls"));
        this.root = props.getProperty("crawler.root");
        this.domain = props.getProperty("crawler.domain");
        this.curr = this.root;
        nextURLIDScanned++;
        in.close();
    }

    public void openConnection() throws SQLException, IOException {
        String drivers = props.getProperty("jdbc.drivers");
        if (drivers != null) System.setProperty("jdbc.drivers", drivers);
        String url = props.getProperty("jdbc.url");
        String username = props.getProperty("jdbc.username");
        String password = props.getProperty("jdbc.password");
        connection = DriverManager.getConnection(url, username, password);
    }

    public void createDB() throws SQLException, IOException {
        this.openConnection();
        Statement stat = connection.createStatement();

        // Delete the table first if any
        try {
            stat.executeUpdate("DROP TABLE urls");
            stat.executeUpdate("DROP TABLE words");
        } catch (Exception e) {
        }

        // Create the table
        stat.executeUpdate("CREATE TABLE urls (urlid INT NOT NULL, url VARCHAR(512), description VARCHAR(275), image VARCHAR(512), PRIMARY KEY(urlid))");
        stat.executeUpdate("CREATE TABLE words (word VARCHAR(100), urlid INT)");
    }

    public boolean urlInDB(String urlFound) {
        try {
            Statement stat = connection.createStatement();
            ResultSet result = stat.executeQuery("SELECT * FROM urls WHERE url LIKE '" + urlFound + "'");
            return result.next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void deleteURLInDB(int urlid) {
        try {
            Statement stat = connection.createStatement();
            String query = "DELETE FROM urls WHERE urlid='" + urlid + "'";
            stat.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertURLInDB(String url) {
        try {
            Statement stat = connection.createStatement();
            String query = "INSERT INTO urls(urlid, url) VALUES ('" + urlID + "','" + url + "');";
            stat.executeUpdate(query);
            this.urlID++;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertImageInDB(int urlid, String imageSrc) {
        try {
            Statement stat = connection.createStatement();
            String query = "UPDATE urls SET image='" + imageSrc + "' WHERE urlid='" + urlid + "';";
            stat.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void insertDescInDB(int urlid, String desc) {
        try {
            Statement stat = connection.createStatement();
            String query = "UPDATE urls SET description='" + desc + "' WHERE urlid='" + urlid + "';";
            stat.executeUpdate(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void grabURLInDB(int urlid) {
        try {
            Statement stat = connection.createStatement();
            ResultSet result = stat.executeQuery("SELECT url FROM urls WHERE urlid = " + Integer.toString(urlid));
            if (result.next()) {
                this.curr = result.getString("url");
            } else {
                this.curr = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // grab the entire page, remove special characters and insert with a giant query
    public void insertWordTable(int urlid, String desc) {
        try {
            desc = desc.replaceAll("[^A-Za-z0-9 ]", "");
            Statement stat = connection.createStatement();
            String[] split = desc.split(" ");
            String query = "";
            Map<String, Integer> wordTable = new HashMap<>();
            for (String w : split) {
                if (w.length() <= 1) {
                    continue;
                } else if (w.length() > 100) {
                    w = w.substring(0, 99);
                }
                w = w.toLowerCase();
                wordTable.put(w, urlid);
            }

            Set<String> keys = wordTable.keySet();
            for (String k : keys) {
                query += "('" + k + "'," + "'" + Integer.toString(wordTable.get(k)) + "'), ";
            }

            if (query.length() > 2) {
                query = query.substring(0, query.length() - 2);
                query = "INSERT INTO words(word, urlid) values" + query + ";";
                stat.executeUpdate(query);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // not necessary, but speeds up crawling since I don't parse the links before adding to the DB
    public boolean goodURL(String url) {
        return
                !url.contains("#") &&
                        !url.contains(".pdf") &&
                        !url.contains(".PDF") &&
                        !url.contains(".jpg") &&
                        !url.contains(".JPG") &&
                        !url.contains(".doc") &&
                        !url.contains(".ppt") &&
                        !url.contains(".txt") &&
                        !url.contains(".gif") &&
                        !url.contains(".m") &&
                        !url.contains(".v") &&
                        !url.contains(".docx");
    }

    // grabs all valid anchor links and inserts in DB
    public void insertAllURLS(Elements links) {
        for (Element link : links) {
            String[] urlSplit = link.attr("abs:href").split("\\?");
            String url = urlSplit[0].replace(" ", "%20").replace("'", "\\'");

            // prevent duplicates because of this slash
            if (url.substring(url.length() - 1).equals("/"))
                url = url.substring(0, url.length() - 1);

            if (url.contains(this.domain) && url.contains("http") && goodURL(url) && !urlInDB(url)) {
                insertURLInDB(url);
                nextURLIDScanned++;
                if (nextURLIDScanned > this.maxURL) {
                    break;
                }
            }
        }
    }

    // first two images are generally the Purdue logos, avoid if possible
    public void insertImage(Elements images) {
        Element image = images.first();
        if (image != null) {
            String imageURL = image.absUrl("src");
            if (imageURL.equals("https://www.cs.purdue.edu/images/logo.svg")) {
                if (images.size() > 2) {
                    Element noLogo = images.get(2);
                    if (image != null) {
                        imageURL = noLogo.absUrl("src");
                    }
                } else {
                    Element noLogo = images.get(1);
                    if (image != null) {
                        imageURL = noLogo.absUrl("src");
                    }
                }
            }
            // spaces for URL, apostrphes for SQL
            imageURL = imageURL.replace(" ", "%20");
            imageURL = imageURL.replace("'", "\\'");
            insertImageInDB(nextURLID - 1, imageURL);
        }
    }

    // fallback = whole body if there is insufficient paragraphs tags
    public void insertDesc(String title, String desc, String fallback) {
        desc = desc.replaceAll("[^A-Za-z0-9 ]", "");
        int len = desc.length();
        if (len > 200) {
            len = 200;
        } else if (len < 100) {
            desc = fallback.replaceAll("[^A-Za-z0-9 ]", "");
            if (desc.length() > 200) len = 200;
        }

        title = title.replaceAll("\\|", "");
        title = title.replaceAll("'", "");
        int tLen = title.length();
        if (tLen > 74) {
            tLen = 74;
        }
        String save = desc.substring(0, len) + "|" + title.substring(0, tLen);
        insertDescInDB(nextURLID - 1, save);
    }

    public void fetchURL(String urlScanned) {
        try {
            urlScanned = urlScanned.replace("\\", "");
            Response res = Jsoup.connect(urlScanned).ignoreContentType(false).timeout(6000).execute();
            if (!res.contentType().contains("text/html") && !res.contentType().contains("text/plain")) {
                System.out.println("==> bad content-type: " + res.contentType());
                deleteURLInDB(nextURLID - 1);
                return;
            }

            Document doc = res.parse();
            if (nextURLIDScanned < this.maxURL) {
                insertAllURLS(doc.select("a[href]"));
            }

            // ignore the root link
            if (nextURLID != 0) {
                insertImage(doc.select("img"));
                insertDesc(doc.select("title").text(), doc.select("p").text(), doc.select("body").text());
                insertWordTable(nextURLID - 1, doc.select("body").text());
            }
        } catch (Exception e) {
            // most likely a timeout or not html/text
            System.out.println("==> remove " + Integer.toString(nextURLID - 1));
            deleteURLInDB(nextURLID - 1);
        }
    }
}

