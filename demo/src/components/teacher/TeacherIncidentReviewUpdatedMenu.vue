<template>
  <div
    :class="isDark ? 'dark' : 'light'"
    class="bg-background-light dark:bg-background-dark font-display text-slate-900 dark:text-slate-100 min-h-screen"
  >
    <div class="layout-container flex h-full grow flex-col">
      <TeacherTopHeader active-section="monitoring" />

      <main class="relative flex-1 max-w-[1280px] mx-auto w-full p-4 lg:p-8 overflow-hidden">
        <div class="pointer-events-none absolute -top-16 -left-16 size-72 rounded-full bg-primary/15 blur-3xl animate-float-slow"></div>
        <div class="pointer-events-none absolute -bottom-24 -right-12 size-80 rounded-full bg-primary/10 blur-3xl animate-float-delay"></div>

        <div class="relative mb-6 animate-fade-up">
          <div class="flex items-center gap-2 text-sm text-slate-500 dark:text-slate-400 mb-2">
            <RouterLink class="hover:text-primary transition-colors flex items-center gap-1" to="/teacher/exams"><span class="material-symbols-outlined text-base">assignment</span> Exams</RouterLink>
            <span class="material-symbols-outlined text-xs">chevron_right</span>
            <span class="text-slate-900 dark:text-slate-100 font-medium">Review History</span>
          </div>
          <div class="flex flex-col md:flex-row md:items-end justify-between gap-4">
            <div>
              <h1 class="text-3xl font-extrabold text-slate-900 dark:text-slate-100 tracking-tight">{{ selectedExamTitle }}</h1>
              <p class="text-slate-600 dark:text-slate-400 mt-1">Review and manage flagged academic integrity violations across all active sessions.</p>
            </div>
            <button class="inline-flex items-center justify-center gap-2 px-4 py-2.5 bg-primary text-white font-bold rounded-lg hover:bg-primary/90 hover:-translate-y-0.5 transition-all duration-200 shadow-sm" type="button">
              <span class="material-symbols-outlined text-xl">download</span>
              <span>Download Report</span>
            </button>
          </div>
        </div>

        <div class="relative grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8 animate-fade-up-delay">
          <div class="bg-white dark:bg-slate-900 p-5 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm hover:-translate-y-0.5 hover:shadow-md transition-all duration-200" v-for="card in summaryCards" :key="card.title">
            <div class="flex items-center justify-between mb-2">
              <p class="text-slate-500 dark:text-slate-400 text-sm font-medium">{{ card.title }}</p>
              <span class="material-symbols-outlined text-primary">{{ card.icon }}</span>
            </div>
            <div v-if="card.sub" class="flex flex-col">
              <h3 class="text-2xl font-bold text-slate-900 dark:text-slate-100 truncate">{{ card.value }}</h3>
              <p class="text-slate-400 text-xs mt-1">{{ card.sub }}</p>
            </div>
            <div v-else class="flex items-baseline gap-2">
              <h3 class="text-2xl font-bold text-slate-900 dark:text-slate-100">{{ card.value }}</h3>
              <span :class="card.trendClass" class="text-sm font-bold flex items-center">{{ card.trend }}</span>
            </div>
          </div>
        </div>

        <div class="bg-white dark:bg-slate-900 p-4 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm mb-6">
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            <div class="flex flex-col gap-1.5">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Date Range</label>
              <select class="form-select w-full bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700 rounded-lg text-sm focus:ring-primary focus:border-primary">
                <option>Last 30 Days</option>
                <option>Last 7 Days</option>
                <option>Last 24 Hours</option>
              </select>
            </div>
            <div class="flex flex-col gap-1.5">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Violation Type</label>
              <select class="form-select w-full bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700 rounded-lg text-sm focus:ring-primary focus:border-primary">
                <option>All Violations</option>
                <option>Tab Switching</option>
                <option>AI Detection</option>
                <option>External Device</option>
              </select>
            </div>
            <div class="flex flex-col gap-1.5 lg:col-span-2">
              <label class="text-xs font-bold text-slate-500 uppercase tracking-wider">Search</label>
              <input class="form-input w-full bg-slate-50 dark:bg-slate-800 border-slate-200 dark:border-slate-700 rounded-lg text-sm focus:ring-primary focus:border-primary" placeholder="Search student name or ID..." />
            </div>
          </div>
        </div>

        <div class="bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 shadow-sm overflow-hidden">
          <div class="overflow-x-auto">
            <table class="w-full text-left border-collapse">
              <thead>
                <tr class="bg-slate-50 dark:bg-slate-800/50 border-b border-slate-200 dark:border-slate-800">
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Student</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">ID</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Incident Time</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Violation</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider">Action</th>
                  <th class="px-6 py-4 text-xs font-bold text-slate-500 uppercase tracking-wider text-right">Details</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-slate-100 dark:divide-slate-800">
                <tr v-for="incident in incidents" :key="incident.studentId" class="hover:bg-slate-50 dark:hover:bg-slate-800/30 transition-colors">
                  <td class="px-6 py-4 text-sm font-bold text-slate-900 dark:text-slate-100">{{ incident.student }}</td>
                  <td class="px-6 py-4 text-xs text-slate-500">{{ incident.studentId }}</td>
                  <td class="px-6 py-4">
                    <div class="flex flex-col">
                      <span class="text-sm text-slate-900 dark:text-slate-100">{{ incident.date }}</span>
                      <span class="text-xs text-slate-500">{{ incident.time }}</span>
                    </div>
                  </td>
                  <td class="px-6 py-4">
                    <span :class="incident.violationClass" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold border">
                      {{ incident.violation }}
                    </span>
                  </td>
                  <td class="px-6 py-4 text-sm text-slate-600 dark:text-slate-400 italic">{{ incident.action }}</td>
                  <td class="px-6 py-4 text-right">
                    <button class="text-primary hover:bg-primary/10 px-3 py-1.5 rounded-lg text-sm font-bold transition-all inline-flex items-center gap-1 bg-primary/5" type="button" @click="openIncidentReport(incident)">
                      <span>Review</span>
                      <span class="material-symbols-outlined text-lg">open_in_new</span>
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>

    <div v-if="showIncidentModal && selectedIncident" class="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 backdrop-blur-sm p-4">
      <div class="bg-white dark:bg-slate-900 w-full max-w-4xl max-h-[90vh] overflow-hidden rounded-xl shadow-2xl flex flex-col border border-slate-200 dark:border-slate-700">
        <header class="flex items-center justify-between border-b border-slate-200 dark:border-slate-700 px-8 py-5 shrink-0 bg-white dark:bg-slate-900">
          <div class="flex items-center gap-3">
            <div class="bg-primary/10 p-2 rounded-lg">
              <span class="material-symbols-outlined text-primary">report</span>
            </div>
            <h2 class="text-xl font-bold text-slate-900 dark:text-white leading-tight">Incident Report: {{ selectedIncident.student }}</h2>
          </div>
          <button class="flex items-center justify-center rounded-lg h-10 w-10 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors text-slate-500" type="button" @click="closeIncidentReport">
            <span class="material-symbols-outlined">close</span>
          </button>
        </header>

        <div class="px-8 pt-5 bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-700">
          <div class="flex gap-2">
            <button type="button" @click="activeReportTab = 'result'" :class="activeReportTab === 'result' ? 'bg-primary text-white' : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300'" class="px-4 py-2 rounded-lg text-sm font-bold transition-colors">
              Kết quả
            </button>
            <button type="button" @click="activeReportTab = 'warnings'" :class="activeReportTab === 'warnings' ? 'bg-primary text-white' : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300'" class="px-4 py-2 rounded-lg text-sm font-bold transition-colors">
              Số lượt cảnh báo
            </button>
          </div>
        </div>

        <div class="overflow-y-auto p-8 space-y-8 bg-background-light/30 dark:bg-background-dark/30">
          <section v-if="activeReportTab === 'result'" class="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div class="col-span-1 md:col-span-2 grid grid-cols-2 gap-y-4 gap-x-8 bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700">
              <div class="space-y-1">
                <p class="text-slate-500 text-xs font-semibold uppercase tracking-wider">Student Name</p>
                <p class="text-slate-900 dark:text-slate-100 font-medium">{{ selectedIncident.student }}</p>
              </div>
              <div class="space-y-1">
                <p class="text-slate-500 text-xs font-semibold uppercase tracking-wider">Student ID</p>
                <p class="text-slate-900 dark:text-slate-100 font-medium">{{ selectedIncident.studentId }}</p>
              </div>
              <div class="space-y-1 border-t border-slate-100 dark:border-slate-700 pt-3 mt-1">
                <p class="text-slate-500 text-xs font-semibold uppercase tracking-wider">Exam Title</p>
                <p class="text-slate-900 dark:text-slate-100 font-medium text-sm">{{ selectedExamTitle }}</p>
              </div>
              <div class="space-y-1 border-t border-slate-100 dark:border-slate-700 pt-3 mt-1">
                <p class="text-slate-500 text-xs font-semibold uppercase tracking-wider">Date</p>
                <p class="text-slate-900 dark:text-slate-100 font-medium">{{ selectedIncident.date }}</p>
              </div>
              <div class="space-y-1 border-t border-slate-100 dark:border-slate-700 pt-3 mt-1">
                <p class="text-slate-500 text-xs font-semibold uppercase tracking-wider">Proctor Name</p>
                <p class="text-slate-900 dark:text-slate-100 font-medium">Sarah Smith</p>
              </div>
              <div class="space-y-1 border-t border-slate-100 dark:border-slate-700 pt-3 mt-1">
                <p class="text-slate-500 text-xs font-semibold uppercase tracking-wider">Result</p>
                <span :class="selectedIncident.resultClass" class="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold">
                  {{ selectedIncident.result }}
                </span>
              </div>
            </div>
            <div class="flex flex-col gap-4">
              <div class="flex-1 flex flex-col justify-center items-center gap-1 rounded-xl p-4 border border-red-200 dark:border-red-900/50 bg-red-50 dark:bg-red-950/20">
                <p class="text-red-800 dark:text-red-400 text-xs font-bold uppercase">Severity Level</p>
                <p class="text-red-600 dark:text-red-300 tracking-tight text-2xl font-black">{{ selectedIncident.severity }}</p>
              </div>
              <div class="flex-1 flex flex-col justify-center items-center gap-1 rounded-xl p-4 border border-primary/20 bg-primary/5">
                <p class="text-primary text-xs font-bold uppercase">Violation Type</p>
                <p class="text-slate-900 dark:text-white text-center text-sm font-bold leading-tight">{{ selectedIncident.violation }}</p>
              </div>
            </div>
          </section>

          <section v-else class="space-y-4">
            <div class="bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700">
              <h3 class="text-slate-900 dark:text-white font-bold flex items-center gap-2 mb-4">
                <span class="material-symbols-outlined text-primary">timeline</span>
                Incident Timeline
              </h3>
              <div class="relative space-y-6">
                <div class="absolute left-[11px] top-2 bottom-2 w-0.5 bg-slate-200 dark:bg-slate-700"></div>
                <div v-for="event in selectedIncident.warningTimeline" :key="event.time + event.label" class="relative flex gap-4">
                  <div :class="event.dotClass" class="z-10 w-6 h-6 rounded-full flex items-center justify-center ring-4 ring-white dark:ring-slate-800">
                    <span class="material-symbols-outlined text-[14px] text-white">{{ event.icon }}</span>
                  </div>
                  <div>
                    <p :class="event.timeClass" class="text-xs font-bold uppercase tracking-wide">{{ event.time }}</p>
                    <p class="text-sm font-semibold text-slate-900 dark:text-slate-100">{{ event.label }}</p>
                    <p v-if="event.sub" class="text-xs text-slate-500">{{ event.sub }}</p>
                  </div>
                </div>
              </div>
            </div>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div class="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 text-center">
                <p class="text-slate-500 text-xs font-bold uppercase tracking-wider">Total Warnings</p>
                <p class="text-3xl font-black text-red-600 mt-1">{{ selectedIncident.warningCount }}</p>
              </div>
              <div class="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 text-center">
                <p class="text-slate-500 text-xs font-bold uppercase tracking-wider">First Warning</p>
                <p class="text-lg font-bold mt-1">{{ selectedIncident.warningTimeline[1].time }}</p>
              </div>
              <div class="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 text-center">
                <p class="text-slate-500 text-xs font-bold uppercase tracking-wider">Latest Warning</p>
                <p class="text-lg font-bold mt-1">{{ selectedIncident.time }}</p>
              </div>
            </div>
          </section>
        </div>

        <footer class="flex items-center justify-end gap-3 border-t border-slate-200 dark:border-slate-700 px-8 py-5 shrink-0 bg-slate-50 dark:bg-slate-900/80">
          <button class="flex items-center gap-2 px-4 py-2 text-sm font-bold text-slate-700 dark:text-slate-300 border border-slate-300 dark:border-slate-600 rounded-lg hover:bg-white dark:hover:bg-slate-800 transition-colors" type="button">
            <span class="material-symbols-outlined text-lg">print</span>
            Print Report
          </button>
          <button class="flex items-center gap-2 px-5 py-2 text-sm font-bold text-white bg-primary rounded-lg hover:bg-primary/90 transition-shadow shadow-sm" type="button">
            <span class="material-symbols-outlined text-lg">download</span>
            Download PDF
          </button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import TeacherTopHeader from './TeacherTopHeader.vue'

const route = useRoute()
const isDark = ref(false)
const showIncidentModal = ref(false)
const activeReportTab = ref('result')
const selectedIncident = ref(null)

const selectedExamTitle = computed(() => route.query.title || 'Selected Exam')

const openIncidentReport = (incident) => {
  selectedIncident.value = incident
  activeReportTab.value = 'result'
  showIncidentModal.value = true
}

const closeIncidentReport = () => {
  showIncidentModal.value = false
}

const summaryCards = [
  { title: 'Total Incidents (Month)', icon: 'warning', value: '142', trend: '▲ 12%', trendClass: 'text-emerald-600 dark:text-emerald-400' },
  { title: 'Common Violation', icon: 'tab_unselected', value: 'Tab Switching', sub: '45% of total flags' },
  { title: 'Incidents Rate', icon: 'analytics', value: '2.4%', trend: '▼ 0.8%', trendClass: 'text-rose-600 dark:text-rose-400' },
  { title: 'Resolved Issues', icon: 'check_circle', value: '128', trend: 'of 142 cases', trendClass: 'text-slate-400' }
]

const incidents = [
  {
    student: 'David Miller',
    studentId: '2024-DM-081',
    date: 'Oct 24, 2023',
    time: '10:45 AM',
    violation: 'AI Usage Detection',
    violationClass: 'bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-400 border-rose-200 dark:border-rose-800',
    action: 'Exam Invalidated',
    result: 'Disqualified',
    resultClass: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
    severity: 'HIGH',
    warningCount: 6,
    warningTimeline: [
      { time: '10:15 AM', label: 'Exam Started', sub: '', icon: 'play_arrow', dotClass: 'bg-primary', timeClass: 'text-slate-400' },
      { time: '10:28 AM', label: 'AI pattern detected', sub: 'System warning issued', icon: 'warning', dotClass: 'bg-amber-500', timeClass: 'text-amber-500' },
      { time: '10:36 AM', label: 'Second offense', sub: 'Escalated for review', icon: 'person_alert', dotClass: 'bg-red-500', timeClass: 'text-red-500' },
      { time: '10:45 AM', label: 'Exam invalidated', sub: '', icon: 'block', dotClass: 'bg-slate-900 dark:bg-black', timeClass: 'text-slate-900 dark:text-white' }
    ]
  },
  {
    student: 'Sarah Jenkins',
    studentId: '2024-SJ-112',
    date: 'Oct 24, 2023',
    time: '09:12 AM',
    violation: 'Tab Switching',
    violationClass: 'bg-amber-100 text-amber-800 dark:bg-amber-900/40 dark:text-amber-400 border-amber-200 dark:border-amber-800',
    action: 'Warning Issued',
    result: 'Under Review',
    resultClass: 'bg-amber-100 text-amber-800 dark:bg-amber-900/30 dark:text-amber-400',
    severity: 'MEDIUM',
    warningCount: 3,
    warningTimeline: [
      { time: '08:50 AM', label: 'Exam Started', sub: '', icon: 'play_arrow', dotClass: 'bg-primary', timeClass: 'text-slate-400' },
      { time: '09:00 AM', label: '1st Tab Switch Detected', sub: 'System warning issued', icon: 'warning', dotClass: 'bg-amber-500', timeClass: 'text-amber-500' },
      { time: '09:08 AM', label: '2nd Tab Switch', sub: 'Proctor notified', icon: 'person_alert', dotClass: 'bg-red-500', timeClass: 'text-red-500' },
      { time: '09:12 AM', label: 'Review opened', sub: '', icon: 'visibility', dotClass: 'bg-slate-900 dark:bg-black', timeClass: 'text-slate-900 dark:text-white' }
    ]
  },
  {
    student: 'Marcus Wright',
    studentId: '2024-MW-055',
    date: 'Oct 23, 2023',
    time: '02:30 PM',
    violation: 'External Device',
    violationClass: 'bg-rose-100 text-rose-800 dark:bg-rose-900/40 dark:text-rose-400 border-rose-200 dark:border-rose-800',
    action: 'Exam Invalidated',
    result: 'Disqualified',
    resultClass: 'bg-red-100 text-red-800 dark:bg-red-900/30 dark:text-red-400',
    severity: 'HIGH',
    warningCount: 5,
    warningTimeline: [
      { time: '02:00 PM', label: 'Exam Started', sub: '', icon: 'play_arrow', dotClass: 'bg-primary', timeClass: 'text-slate-400' },
      { time: '02:11 PM', label: 'Device detection event', sub: 'Warning issued', icon: 'devices', dotClass: 'bg-amber-500', timeClass: 'text-amber-500' },
      { time: '02:21 PM', label: 'Repeated external signal', sub: 'High confidence detection', icon: 'link', dotClass: 'bg-red-500', timeClass: 'text-red-500' },
      { time: '02:30 PM', label: 'Exam Automatically Terminated', sub: '', icon: 'block', dotClass: 'bg-slate-900 dark:bg-black', timeClass: 'text-slate-900 dark:text-white' }
    ]
  }
]
</script>

<style scoped>
.font-display {
  font-family: 'Inter', sans-serif;
}

@keyframes fadeUp {
  from {
    opacity: 0;
    transform: translateY(18px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes floatSlow {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(0, -14px, 0);
  }
}

@keyframes floatDelay {
  0%,
  100% {
    transform: translate3d(0, 0, 0);
  }
  50% {
    transform: translate3d(0, 12px, 0);
  }
}

.animate-fade-up {
  animation: fadeUp 0.5s ease-out;
}

.animate-fade-up-delay {
  animation: fadeUp 0.65s ease-out;
}

.animate-float-slow {
  animation: floatSlow 7s ease-in-out infinite;
}

.animate-float-delay {
  animation: floatDelay 8s ease-in-out infinite;
}
</style>
