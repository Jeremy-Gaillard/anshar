/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package no.rutebanken.anshar.validation;

import no.rutebanken.anshar.routes.validation.validators.ParticipantRefValidator;
import org.junit.BeforeClass;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;

public class ParticipantRefValidatorTest extends CustomValidatorTest {

    static ParticipantRefValidator validator;

    @BeforeClass
    public static void init() {
        validator = new ParticipantRefValidator();
    }

    @Test
    public void testValidParticipant() throws Exception{
        String xml = createXml("ParticipantRef", "ENT");

        assertNull("Valid ParticipantRef flagged as invalid", validator.isValid(createXmlNode(xml)));
    }

    @Test
    public void testInvalidParticipant() throws Exception{
        String xml = createXml("ParticipantRef", "N");

        assertNotNull("Invalid ParticipantRef flagged as valid", validator.isValid(createXmlNode(xml)));
    }
}
