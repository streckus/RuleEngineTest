/*
 * Defines the typing annotation for an RProper rule.
 *
 * This file is part of the Information System on Graph Classes and their
 * Inclusions (ISGCI) at http://www.graphclasses.org.
 * Email: isgci@graphclasses.org
 */

package teo.isgci.appl.deducer;

import java.lang.annotation.*;

/**
 * Gives the expected types for class when invoking the rule.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RProperTyping {
    Class type();
}

/* EOF */

