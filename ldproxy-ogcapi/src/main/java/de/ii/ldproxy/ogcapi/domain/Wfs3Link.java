/**
 * Copyright 2019 interactive instruments GmbH
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package de.ii.ldproxy.ogcapi.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import java.awt.*;

/**
 * @author zahnen
 */
@Value.Immutable
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(as = ImmutableWfs3Link.class)
@XmlType(propOrder={"rel","type","title", "href","templated"})
public abstract class Wfs3Link {

    @Nullable
    @XmlAttribute
    public abstract String getRel();

    @Nullable
    @XmlAttribute
    public abstract String getType();

    @Nullable
    @XmlAttribute
    @Value.Derived
    public String getTitle() {
        if (getDescription() == null) {
            return null;
        }
        if (getTypeLabel() == null) {
            return getDescription();
        }
        return String.format("%s as %s", getDescription(), getTypeLabel());
    }

    @XmlAttribute
    public abstract String getHref();

    @Nullable
    @JsonIgnore
    @XmlTransient
    public abstract String getTypeLabel();

    @Nullable
    abstract String getDescription();

    @Nullable
    @XmlAttribute
    public abstract String getTemplated();

    /*@XmlAttribute
    public String href;
    @XmlAttribute
    public String rel;
    @XmlAttribute
    public String type;
    @XmlAttribute
    public String title;*/

    //public Wfs3Link() {}

    //public Wfs3Link(String href, String rel, String type, String title) {
        //this.href = href;
        //this.rel = rel;
        //this.type = type;
        //this.title = title;
    //}
}