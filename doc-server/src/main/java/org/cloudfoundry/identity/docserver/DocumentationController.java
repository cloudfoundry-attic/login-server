package org.cloudfoundry.identity.docserver;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.io.IOException;

import static org.apache.commons.io.FileUtils.readFileToString;

@Controller
public class DocumentationController {

    @RequestMapping("/api-docs")
    @ResponseBody
    public String apiDocs() throws IOException {
        return readFileToString(new File(getDocumentationRoot(), "api-docs.json"));
    }

    @RequestMapping("/api-docs/{api}")
    @ResponseBody
    public String api(@PathVariable String api) throws IOException {
        return readFileToString(new File(getDocumentationRoot(), "api-docs/" + api + ".json"));
    }

    private File getDocumentationRoot() throws IOException {
        return new ClassPathResource("api-docs").getFile();
    }
}
