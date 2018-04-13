package no.rutebanken.anshar.routes.validation.validators;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.bind.ValidationEvent;
import javax.xml.bind.helpers.ValidationEventImpl;
import javax.xml.bind.helpers.ValidationEventLocatorImpl;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

public abstract class CustomValidator {

    public abstract String getXpath();
    public abstract ValidationEvent isValid(Node node);

    /**
     * Returns the textual content of the provided node - null if it does not exist
     * @param node
     * @return
     */
    protected String getNodeValue(Node node) {
        if (node != null && node.getFirstChild() != null && node.getFirstChild().getNodeValue() != null) {
            return node.getFirstChild().getNodeValue();
        }
        return null;
    }

    /**
     * Returns the textual content of the provided node - null if it does not exist
     * @param node
     * @return
     */
    protected String getNodeAttributeValue(Node node, String attributeName) {
        if (node != null && node.getAttributes() != null) {
            final NamedNodeMap attributes = node.getAttributes();
            return getNodeValue(attributes.getNamedItem(attributeName));
        }
        return null;
    }

    protected String getChildNodeValue(Node node, String name) {
        return getNodeValue(getChildNodeByName(node, name));
    }

    protected Node getChildNodeByName(Node node, String name) {
        if (node != null) {
            final NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node n = childNodes.item(i);
                if (n.hasChildNodes()) {
                    for (int j = 0; j < n.getChildNodes().getLength(); j++) {
                        if (n.getNodeName().equals(name)) {
                            return n;
                        }
                    }
                }
            }
        }
        return null;
    }

    protected List<Node> getChildNodesByName(Node node, String name) {
        List<Node> nodes = new ArrayList<>();
        if (node != null) {
            final NodeList childNodes = node.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node n = childNodes.item(i);
                if (n.getNodeName().equals(name)) {
                    nodes.add(n);
                }
            }
        }
        return nodes;
    }


    protected String getSiblingNodeValue(Node node, String name) {
        return getNodeValue(getSiblingNodeByName(node, name));
    }

    protected Node getSiblingNodeByName(Node node, String name) {
        final Node parentNode = node.getParentNode();
        if (parentNode != null) {
            final NodeList childNodes = parentNode.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node n = childNodes.item(i);
                if (n.getNodeName().equals(name)) {
                    return n;
                }
            }
        }
        return null;
    }

    protected List<Node> getSiblingNodesByName(Node node, String name) {
        final Node parentNode = node.getParentNode();
        List<Node> nodes = new ArrayList<>();
        if (parentNode != null) {
            final NodeList childNodes = parentNode.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                final Node n = childNodes.item(i);
                if (n.getNodeName().equals(name)) {
                    nodes.add(n);
                }
            }
        }
        return nodes;
    }

    /**
     *
     * @param node Node that is validated
     * @param fieldname Name of attribute that fails validation
     * @param expectedValues Expected value or description of expected value
     * @param actualValue Actual value of node
     * @param severity
     * @return
     */
    protected ValidationEvent createEvent(Node node, String fieldname, Object expectedValues, String actualValue, int severity) {
        String message = MessageFormat.format("Value [{0}] is invalid for field [{1}], expected {2}", actualValue, fieldname, expectedValues);
        return new ValidationEventImpl(severity, message, new ValidationEventLocatorImpl(node));
    }
}
