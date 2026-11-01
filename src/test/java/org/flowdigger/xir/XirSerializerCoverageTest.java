package org.flowdigger.xir;

import org.flowdigger.xir.XirParser;
import org.flowdigger.xir.XirSerializer;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import javax.xml.parsers.SAXParserFactory;
import java.io.StringReader;
import java.io.StringWriter;

public class XirSerializerCoverageTest {

    @Test
    public void rendersLegacyModeShapesWhenRequested() throws Exception {
        String input = """
            (.
              (book { xmlns:m "urn:math" id "b1" }
                (! "legacy-comment")
                (?xml-stylesheet { type "text/xsl" href "style.xsl" })
                (m:title "XML")))
            """;

        StringWriter writer = new StringWriter();
        XirSerializer serializer = new XirSerializer(
            writer,
            XirSerializer.OutputFormat.BEAUTIFIED,
            XirSerializer.SyntaxMode.LEGACY
        );

        new XirParser().parse(new StringReader(input), serializer, serializer);
        String output = writer.toString();

        Assert.assertTrue(output.contains("[id \"b1\"]"));
        Assert.assertTrue(output.contains("[ns \"m\" \"urn:math\"]") || output.contains("[ns \"\" \"urn:math\"]"));
        Assert.assertTrue(output.contains("(# \"legacy-comment\")"));
        Assert.assertTrue(output.contains("(?xml-stylesheet type=\"text/xsl\" href=\"style.xsl\")"));
    }

    @Test
    public void rendersEmptyCanonicalDocumentWhenNoEvents() throws Exception {
        StringWriter writer = new StringWriter();
        XirSerializer serializer = new XirSerializer(
            writer,
            XirSerializer.OutputFormat.BEAUTIFIED,
            XirSerializer.SyntaxMode.CANONICAL
        );

        serializer.startDocument();
        serializer.endDocument();

        Assert.assertEquals("(.)" + System.lineSeparator(), writer.toString());
    }

    @Test
    public void keepsRawPiDataWhenNotTokenized() throws Exception {
        StringWriter writer = new StringWriter();
        XirSerializer serializer = new XirSerializer(
            writer,
            XirSerializer.OutputFormat.COMPACT,
            XirSerializer.SyntaxMode.CANONICAL
        );

        serializer.startDocument();
        serializer.processingInstruction("target", "raw-data without-equals");
        serializer.endDocument();

        String output = writer.toString();
        Assert.assertTrue(output.contains("(?target {data \"raw-data without-equals\"})"));
    }

    @Test
    public void serializesXPathFunctionsMapInStrictMode() throws Exception {
        String xml = """
            <map xmlns="http://www.w3.org/2005/xpath-functions">
              <string key="name">Jurgen</string>
              <string key="fullName">Jurgen S. Hildebrand</string>
              <number key="age">42</number>
              <boolean key="hasBike">true</boolean>
            </map>
            """;

        StringWriter writer = new StringWriter();
        XirSerializer serializer = new XirSerializer(
            writer,
            XirSerializer.OutputFormat.BEAUTIFIED,
            XirSerializer.SyntaxMode.CANONICAL,
            XirSerializer.AutoTypingMode.STRICT_STRING
        );
        parseXmlToSerializer(xml, serializer);

        String output = writer.toString();
        Assert.assertTrue(output.contains("name Jurgen"));
        Assert.assertTrue(output.contains("fullName \"Jurgen S. Hildebrand\""));
        Assert.assertTrue(output.contains("age 42"));
        Assert.assertTrue(output.contains("hasBike true"));
    }

    @Test
    public void serializesXPathFunctionsArrayAndListSynonym() throws Exception {
        String xml = """
            <array xmlns="http://www.w3.org/2005/xpath-functions">
              <string>XSLT</string>
              <string>XPath</string>
              <string>Schematron</string>
              <string>Java</string>
              <string>Common Lisp</string>
            </array>
            """;

        StringWriter writer = new StringWriter();
        XirSerializer serializer = new XirSerializer(
            writer,
            XirSerializer.OutputFormat.BEAUTIFIED,
            XirSerializer.SyntaxMode.CANONICAL
        );
        parseXmlToSerializer(xml, serializer);
        String output = writer.toString();
        Assert.assertTrue(output.contains("["));
        Assert.assertTrue(output.contains("XSLT"));
        Assert.assertTrue(output.contains("\"Common Lisp\""));

        String listXml = xml.replace("<array", "<list").replace("</array>", "</list>");
        StringWriter listWriter = new StringWriter();
        parseXmlToSerializer(listXml, new XirSerializer(listWriter, XirSerializer.OutputFormat.COMPACT));
        Assert.assertTrue(listWriter.toString().contains("[XSLT XPath Schematron Java \"Common Lisp\"]"));
    }

    @Test
    public void promotesStringLexemesWhenPromoteModeEnabled() throws Exception {
        String xml = """
            <array xmlns="http://www.w3.org/2005/xpath-functions">
              <string>true</string>
              <string>42</string>
              <string>alpha</string>
            </array>
            """;
        StringWriter writer = new StringWriter();
        XirSerializer serializer = new XirSerializer(
            writer,
            XirSerializer.OutputFormat.COMPACT,
            XirSerializer.SyntaxMode.CANONICAL,
            XirSerializer.AutoTypingMode.PROMOTE
        );
        parseXmlToSerializer(xml, serializer);
        String output = writer.toString();
        Assert.assertTrue(output.contains("[true 42 alpha]"));
    }

    @Test
    public void preservesLegacyCompactAttributeAndNamespaceBlocks() throws Exception {
        String input = """
            (book
              { xmlns:m "urn:math" id "b1" }
              (m:title "XML"))
            """;

        StringWriter writer = new StringWriter();
        XirSerializer serializer = new XirSerializer(
            writer,
            XirSerializer.OutputFormat.COMPACT,
            XirSerializer.SyntaxMode.LEGACY
        );

        new XirParser().parse(new StringReader(input), serializer, serializer);
        String output = writer.toString();
        Assert.assertTrue(output.contains("[id \"b1\"]"));
        Assert.assertTrue(output.contains("[ns \"m\" \"urn:math\"]"));
    }

    @Test
    public void serializesFunctionNamespaceTypedAndStructuredEntries() throws Exception {
        String xml = """
            <map xmlns="http://www.w3.org/2005/xpath-functions">
              <number key="badNum">not-a-number</number>
              <boolean key="badBool">maybe</boolean>
              <null key="nil">ignored</null>
              <array key="skills">
                <string>XSLT</string>
                <number>12</number>
                <boolean>true</boolean>
                <null/>
              </array>
              <map key="profile">
                <string key="name">Jurgen</string>
              </map>
            </map>
            """;

        StringWriter writer = new StringWriter();
        XirSerializer serializer = new XirSerializer(
            writer,
            XirSerializer.OutputFormat.COMPACT,
            XirSerializer.SyntaxMode.CANONICAL
        );
        parseXmlToSerializer(xml, serializer);

        String output = writer.toString();
        Assert.assertTrue(output, output.contains("badNum \"not-a-number\""));
        Assert.assertTrue(output, output.contains("badBool \"maybe\""));
        Assert.assertTrue(output, output.contains("nil null"));
        Assert.assertTrue(output, output.contains("skills"));
        Assert.assertTrue(output, output.contains("profile"));
    }

    @Test
    public void defaultConstructorUsesStrictStringAutotyping() throws Exception {
        String xml = """
            <array xmlns="http://www.w3.org/2005/xpath-functions">
              <string>true</string>
              <string>42</string>
            </array>
            """;
        StringWriter writer = new StringWriter();
        XirSerializer serializer = new XirSerializer(writer);
        parseXmlToSerializer(xml, serializer);
        String output = writer.toString();
        Assert.assertTrue(output.contains("[\"true\" \"42\"]"));
    }

    @Test
    public void rendersLegacyAttributeBlockCompact() throws Exception {
        String xml = """
            <book xmlns:m="urn:math" id="b1" version="1.0">
              <m:title>XML</m:title>
            </book>
            """;

        StringWriter writer = new StringWriter();
        XirSerializer serializer = new XirSerializer(
            writer,
            XirSerializer.OutputFormat.COMPACT,
            XirSerializer.SyntaxMode.LEGACY
        );
        parseXmlToSerializer(xml, serializer);

        String output = writer.toString();
        Assert.assertTrue(output.contains("[id \"b1\" version \"1.0\"]"));
    }

    @Test
    public void rendersBeautifiedAttributeAndNamespaceBlocks() throws Exception {
        String xml = """
            <book xmlns:m="urn:math" id="b1">
              <m:title>XML</m:title>
            </book>
            """;

        StringWriter writer = new StringWriter();
        XirSerializer serializer = new XirSerializer(
            writer,
            XirSerializer.OutputFormat.BEAUTIFIED,
            XirSerializer.SyntaxMode.LEGACY
        );
        parseXmlToSerializer(xml, serializer);

        String output = writer.toString();
        Assert.assertTrue(output.contains("id \"b1\""));
    }

    @Test
    public void handlesFnArrayWithMixedPrimitiveTypes() throws Exception {
        String xml = """
            <array xmlns="http://www.w3.org/2005/xpath-functions">
              <number>42</number>
              <boolean>true</boolean>
              <string>text</string>
              <null/>
            </array>
            """;

        StringWriter writer = new StringWriter();
        XirSerializer serializer = new XirSerializer(
            writer,
            XirSerializer.OutputFormat.COMPACT,
            XirSerializer.SyntaxMode.CANONICAL
        );
        parseXmlToSerializer(xml, serializer);

        String output = writer.toString();
        Assert.assertTrue(output.contains("[42 true text null]"));
    }

    private void parseXmlToSerializer(String xml, XirSerializer serializer) throws Exception {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        XMLReader reader = factory.newSAXParser().getXMLReader();
        reader.setContentHandler(serializer);
        reader.setProperty("http://xml.org/sax/properties/lexical-handler", serializer);
        reader.parse(new InputSource(new StringReader(xml)));
    }
}
