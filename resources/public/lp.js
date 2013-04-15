(function() {
  
  var task = [];
  
  function poll(id, version) {
    $.getJSON("/status?" + $.param({id: id, version: version, timeout: 1000}), function(d) {
      var v = version,
          run = true;
      if (d.result === "update") {
        console.log("update", d);
        var values = d.data.value;
        for (var i = 0; i < 10; i++) {
          task[i].text(values[i]);
        }
        v = d.data.version;
        run = (d.data.status === "run");
      }
      if (run) {
        poll(id, v);
      } else {
        $("#start").prop("disabled", false);
        $("#status").show();
        console.log("DONE!");
      }
    });
  }

  function start() {
    $("#start").prop("disabled", true);
    $("#status").hide();
    for (var i = 0; i < 10; i++) {
      task[i].text("working")
    }
    $.post("/start", {}, function(data) {
      var d = $.parseJSON(data);
      console.log("started:", d.id);
      poll(d.id, d.version);
    });
  }

  $(function() {
    $("#start").click(start);
    var table = $("#results tbody");
    for (var i = 0; i < 10; i++) {
      var t = $("<td>");
      table.append($("<tr>")
                     .append($("<td>").text(i))
                     .append(t));
      task.push(t);
    }
  });  
}());

