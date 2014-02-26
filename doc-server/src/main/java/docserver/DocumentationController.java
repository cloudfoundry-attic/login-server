package docserver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;

import static org.apache.commons.io.FileUtils.*;

@RestController
@PropertySource("classpath:spring.properties")
public class DocumentationController {

    private final String swaggerDocRoot;

    @Autowired
    public DocumentationController(@Value("${swagger_doc_root}") String swaggerDocRoot) {
        this.swaggerDocRoot = swaggerDocRoot;
    }

    @RequestMapping("/api-docs")
    public String apiDocs() throws IOException {
        return readFileToString(new File(getDocumentationRoot(), "api-docs.json"));
    }

    @RequestMapping("/api-docs/{api}")
    public String api(@PathVariable String api) throws IOException {
        return readFileToString(new File(getDocumentationRoot(), "api-docs/" + api + ".json"));
    }

    private File getDocumentationRoot() {
        return new File(swaggerDocRoot);
    }
}
