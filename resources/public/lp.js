(function() {
  
  var r = [];
  
  function poll(id, version) {
    $.getJSON("/status?" + $.param({id: id, version: version, timeout: 1000}), function(d) {
      var v = version,
         run = true;
      if (d.result === "update") {
        var values = d.data.value;
        for (var i = 0; i < 10; i++) {
          r[i].css("width", values[i] * 10);
        }
        v = d.data.version;
        run = (d.data.status === "run");
      }
      if (run) {
        poll(id, v);
      } else {
        $("#start").prop("disabled", false);
        console.log("DONE!");
      }
    });
  }

  function start() {
    $("#start").prop("disabled", true);
    for (var i = 0; i < 10; i++) {
      r[i].css("width", 0);
    }
    $.post("/start", {}, function(data) {
      var d = $.parseJSON(data);
      console.log("started:", d.id);
      poll(d.id, d.version);
    });
  }

  $(function() {
    $("#start").click(start);
    var results = $("#results");
    for (var i = 0; i < 10; i++) {
      var d = $("<div>").addClass("result").css("background", "rgb(0,0," + (10 * i) + ")");
      results.append(d);
      r.push(d);
    }
  });  
}());

