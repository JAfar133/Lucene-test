package org.example;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

class LuceneIndexer {
    private final StandardAnalyzer analyzer;
    private final Directory directory;

    public LuceneIndexer() throws IOException {
        this.analyzer = new StandardAnalyzer();
        this.directory = FSDirectory.open(Files.createTempDirectory("lucene-index"));
    }

    public void indexDocuments(List<String> chunks) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        IndexWriter iwriter = new IndexWriter(directory, config);

        for (String chunk : chunks) {
            Document doc = new Document();
            doc.add(new TextField("content", chunk, Field.Store.YES));
            iwriter.addDocument(doc);
        }
        iwriter.close();
        System.out.println("Documents indexed");
    }

    public String search(String prompt, int numHints) throws Exception {
        DirectoryReader ireader = DirectoryReader.open(directory);
        IndexSearcher isearcher = new IndexSearcher(ireader);
        QueryParser parser = new QueryParser("content", analyzer);
        Query query = parser.parse(prompt);
        ScoreDoc[] hits = isearcher.search(query, numHints).scoreDocs;

        StringBuilder data = new StringBuilder();
        for (ScoreDoc hit : hits) {
            Document hitDoc = isearcher.doc(hit.doc);
            data.append(hitDoc.get("content"));
        }

        ireader.close();
        return data.toString();
    }
}
