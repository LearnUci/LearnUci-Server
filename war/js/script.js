var arr = [];
var img = null;

document.getElementById('desc_submit').onclick = updateDesc;
document.getElementById('tour_button').onclick = addPoint;
document.getElementById('tour_img').onchange = updateImg;
document.getElementById('tour_submit').onclick = addTour;

var fileReader = new FileReader();
fileReader.onload = function(e) {
  img = e.target.result;
  alert('Image loaded');
};

function updateDesc() {
  var id = document.getElementById('desc_id').value.split('_')[0];
  var text = document.getElementById('desc_text').innerText;
  alert('/update', sendRequest({
    'req': 'update_point',
    'id': id,
    'text': text
  }));
}
  
function updateImg() {
  var file = document.getElementById('tour_img').files[0];
  fileReader.readAsDataURL(file);
}

function addPoint() {
  var value = document.getElementById('tour_id').value;
  var name = value.substring(value.indexOf('_') + 1);
  value = value.substring(0, value.indexOf('_'));
  arr.push({
    'value': value,
    'name': name
  });
  var str = [];
  for (var i = 0; i < arr.length; i++) {
    str.push(arr[i]['name']);
  }
  document.getElementById('tour_points').innerHTML = str.join('\n');
}

function addTour() {
  var points = [];
  for (var i = 0; i < arr.length; i++) {
    points.push(arr[i].value);
  }
  var name = document.getElementById('tour_name').value;
  var desc = document.getElementById('tour_desc').innerText;
  sendRequest('addtour', {
    'points': '[' + points.join(',') + ']',
    'name': name,
    'desc': desc,
    'img': img
  });
  arr = [];
  img = null;
}
  
function sendRequest(dest, params) {
  var pArr = [];
  for (key in params) {
    if (pArr.length > 0) {
      pArr.push('&');
    }
    pArr.push(encodeURIComponent(key));
    pArr.push('=');
    pArr.push(encodeURIComponent(params[key])); 
  }
    
  var xhr = new XMLHttpRequest();
  xhr.open('POST', dest, false);
  xhr.setRequestHeader('Content-type','application/x-www-form-urlencoded');
  xhr.send(pArr.join(''));
  return xhr.responseText;
}