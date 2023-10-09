<?xml version="1.0" ?>
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns="http://wixtoolset.org/schemas/v4/wxs">
<xsl:output method="xml" version="1.0" />

<xsl:template match="*[local-name()='Wix']">
  <Include>
    <xsl:apply-templates
        select="*//*[local-name()='ComponentGroup' and @Id='ComponentGroup_HeatExe']/*" />
  </Include>
</xsl:template>

<xsl:template match="@*|node()">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()" /> 
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>
