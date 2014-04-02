<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>
<head>
  <title>Swagger UI</title>
  <link href='https://fonts.googleapis.com/css?family=Droid+Sans:400,700' rel='stylesheet' type='text/css'/>
  <link href='<c:url value="/resources/stylesheets/highlight.default.css" />' media='screen' rel='stylesheet' type='text/css'/>
  <link href='<c:url value="/resources/stylesheets/screen.css" />' media='screen' rel='stylesheet' type='text/css'/>
  <script src='<c:url value="/resources/javascripts/lib/shred.bundle.js" />' type="text/javascript"></script>
  <script src='<c:url value="/resources/javascripts/lib/jquery-1.8.0.min.js" />' type='text/javascript'></script>
  <script src='<c:url value="/resources/javascripts/lib/jquery.slideto.min.js" />' type='text/javascript'></script>
  <script src='<c:url value="/resources/javascripts/lib/jquery.wiggle.min.js" />' type='text/javascript'></script>
  <script src='<c:url value="/resources/javascripts/lib/jquery.ba-bbq.min.js" />' type='text/javascript'></script>
  <script src='<c:url value="/resources/javascripts/lib/handlebars-1.0.0.js" />' type='text/javascript'></script>
  <script src='<c:url value="/resources/javascripts/lib/underscore-min.js" />' type='text/javascript'></script>
  <script src='<c:url value="/resources/javascripts/lib/backbone-min.js" />' type='text/javascript'></script>
  <script src='<c:url value="/resources/javascripts/lib/swagger.js" />' type='text/javascript'></script>
  <script src='<c:url value="/resources/javascripts/swagger-ui.js" />' type='text/javascript'></script>
  <script src='<c:url value="/resources/javascripts/lib/highlight.7.3.pack.js" />' type='text/javascript'></script>
  <script type="text/javascript">
    $(function () {
      window.swaggerUi = new SwaggerUi({
      url: "<c:url value='/api-docs' />",
      dom_id: "swagger-ui-container",
      supportedSubmitMethods: ['get', 'post', 'put', 'delete'],
      onComplete: function(swaggerApi, swaggerUi){
        log("Loaded SwaggerUI")
        $('pre code').each(function(i, e) {hljs.highlightBlock(e)});
      },
      onFailure: function(data) {
        log("Unable to Load SwaggerUI");
      },
      docExpansion: "none"
    });

    $('#input_apiKey').change(function() {
      var key = $('#input_apiKey')[0].value;
      log("key: " + key);
      if(key && key.trim() != "") {
        log("added key " + key);
        window.authorizations.add("key", new ApiKeyAuthorization("api_key", key, "query"));
      }
    })
    window.swaggerUi.load();
  });

  </script>
</head>

<body>
<div id='header'>
  <div class="swagger-ui-wrap">
    <a id="logo" href="http://swagger.wordnik.com">swagger</a>

    <form id='api_selector'>
      <div class='input icon-btn'>
        <img id="show-pet-store-icon" src="<c:url value="/resources/images/pet_store_api.png" />" title="Show Swagger Petstore Example Apis">
      </div>
      <div class='input icon-btn'>
        <img id="show-wordnik-dev-icon" src="<c:url value="/resources/images/wordnik_api.png" />" title="Show Wordnik Developer Apis">
      </div>
      <div class='input'><input placeholder="http://example.com/api" id="input_baseUrl" name="baseUrl" type="text"/></div>
      <div class='input'><input placeholder="api_key" id="input_apiKey" name="apiKey" type="text"/></div>
      <div class='input'><a id="explore" href="#">Explore</a></div>
    </form>
  </div>
</div>

<div id="message-bar" class="swagger-ui-wrap">
  &nbsp;
</div>

<div id="swagger-ui-container" class="swagger-ui-wrap">

</div>

</body>

</html>
