# Features HTML (FEATURES_HTML)

The module *Features HTML* may be enabled for every API with a feature provider. It provides the resources *Features* and *Feature* encoded as HTML.

*Features HTML* implements all requirements of conformance class *HTML* of [OGC API - Features - Part 1: Core 1.0](http://www.opengis.net/doc/IS/ogcapi-features-1/1.0#rc_html) for the two mentioned resources.

## Configuration for API

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`noIndexEnabled` |boolean |`true` |Set `noIndex` for all sites to prevent search engines from indexing.
|`schemaOrgEnabled` |boolean |`true` |Enable [schema.org](https://schema.org) annotations for all sites, which are used e.g. by search engines. The annotations are embedded as JSON-LD.
|`collectionDescriptionsInOverview`  |boolean |`true` |Show collection descriptions in *Feature Collections* resource for HTML.
|`layout` |enum |`CLASSIC` |Layout for *Features* and *Feature* resources. Either `CLASSIC` (mainly for simple objects with simple values) or `COMPLEX_OBJECTS` (supports more complex object structures and longer values).

### Example

```yaml
- buildingBlock: FEATURES_HTML
  schemaOrgEnabled: false
  layout: COMPLEX_OBJECTS
```

## Configuration for collection

|Option |Data Type |Default |Description
| --- | --- | --- | ---
|`itemLabelFormat` |string |`{{id}}` |Define how the feature label for HTML is formed. Default is the feature id. Property names in double curly braces will be replaced with the corresponding value.
|`transformations` |object |`{}` |Optional transformations for feature properties for HTML, see [transformations](README.md#transformations).

### Example

```yaml
    - buildingBlock: FEATURES_HTML
      itemLabelFormat: '{{name}}'
      transformations:
        geometry:
          remove: OVERVIEW
        occupancy[].typeOfOccupant:
          remove: OVERVIEW
        occupancy[].numberOfOccupants:
          remove: OVERVIEW
```