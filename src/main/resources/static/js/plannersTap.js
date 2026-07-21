const menuDir = ["", "/member", "/schedule", "/review", "/admin"];

function initPage() {
  const path = window.location.pathname;
  const detail = document.querySelector(".detail");
  const member = document.querySelector(".member");
  const schedule = document.querySelector(".detail-tabs .tab-schedule");
  const review = document.querySelector(".review");
  const admin = document.querySelector(".admin");

  if (path.includes("member")) {
    member.classList.add("sel");
  } else if (path.includes("schedule")) {
    schedule.classList.add("sel");
  } else if (path.includes("review")) {
    review.classList.add("sel");
  } else if (path.includes("admin")) {
    admin.classList.add("sel");
  } else {
    detail.classList.add("sel");
  }
}

function goPage(midx) {
  location.href =
      location.origin + "/planners" + menuDir[midx] + location.search;
}

function tologin(tid) {
  if (confirm("찜하기는 로그인이 필요합니다.\n로그인하시겠습니까?")) {
    location.href =
        location.origin + "/user/login?redirect=/planners?tid=" + tid;
  }
}

function goPlannersList(btn) {
  const {
    page = "",
    keyword = "",
    location = "",
    category = "",
    sort = "",
  } = btn.dataset;
  window.location.href =
      "/planners/list?page=" +
      page +
      "&keyword=" +
      keyword +
      "&location=" +
      location +
      "&category=" +
      category +
      "&sort=" +
      sort;
}

function addFriend(btn) {
  const uid = btn.dataset.uid;
  const name = btn.dataset.name;
  const csrfToken = document.querySelector('meta[name="_csrf"]').content;
  const csrfHeader = document.querySelector(
      'meta[name="_csrf_header"]',
  ).content;
  const url = "/friends/add?receiver=";

  if (confirm("'" + name + "'" + " 님에게 친구요청을 보내시겠습니까?")) {
    fetch(url + uid, {
      method: "POST",
      headers: { [csrfHeader]: csrfToken },
    }).then((res) => {
      if (res.status === 200) {
        alert("친구 요청을 보냈습니다.");
      } else if (res.status === 409) {
        alert("자기 자신은 가장 소중한 친구입니다.");
      } else if (res.status === 400) {
        alert("이미 친구이거나 친구요청을 보낸 사용자입니다.");
      } else {
        alert("예기치 못한 오류 발생");
      }
    });
  }
}

function leaveByTid(tid) {
  const csrfToken = document.querySelector('meta[name="_csrf"]').content;
  const csrfHeader = document.querySelector(
      'meta[name="_csrf_header"]',
  ).content;
  if (confirm("플래너즈를 탈퇴하시겠습니까?")) {
    fetch("/planners/schedule/mylist?tid=" + tid)
        .then((res) => res.json())
        .then((exists) => {
          if (
              exists &&
              !confirm(
                  "해당 플래너즈에 현재 참가중인 일정이 있습니다. 그래도 떠나시겠습니까?",
              )
          ) {
            return;
          }

          return fetch(`/planners/leave/${tid}?exists=${exists}`, {
            method: "POST",
            headers: { [csrfHeader]: csrfToken },
          });
        })
        .then((res) => {
          if (!res) return;
          return res.json();
        })
        .then((data) => {
          if (data == null) return;

          if (Number(data) > 0) {
            alert("탈퇴되었습니다.");
            location.href = "/planners?tid=" + tid;
          } else {
            alert("예기치 못한 오류 발생");
          }
        })
        .catch((err) => alert(err));
  }
}
