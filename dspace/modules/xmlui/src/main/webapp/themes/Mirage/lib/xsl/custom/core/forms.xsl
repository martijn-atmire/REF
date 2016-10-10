<!--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

-->

<!--
    Templates to cover the forms and forms fields.

    Author: art.lowel at atmire.com
    Author: lieven.droogmans at atmire.com
    Author: ben at atmire.com
    Author: Alexey Maslov

-->

<xsl:stylesheet xmlns:i18n="http://apache.org/cocoon/i18n/2.1"
                xmlns:dri="http://di.tamu.edu/DRI/1.0/"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:xlink="http://www.w3.org/TR/xlink/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"
                xmlns:xhtml="http://www.w3.org/1999/xhtml"
                xmlns:mods="http://www.loc.gov/mods/v3"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="i18n dri mets xlink xsl dim xhtml mods dc">

    <xsl:output indent="yes"/>

    <xsl:template match="dri:help" mode="help">
        <!--Only create the <span> if there is content in the <dri:help> node-->
        <xsl:if test="./text() or ./node()">
            <p class="help-block">
                <xsl:apply-templates />
            </p>
        </xsl:if>
    </xsl:template>

    <xsl:template match="dri:help" mode="compositeComponent">
        <span class="composite-help">
            <i18n:text>
                <xsl:apply-templates/>
            </i18n:text></span>
    </xsl:template>

</xsl:stylesheet>
