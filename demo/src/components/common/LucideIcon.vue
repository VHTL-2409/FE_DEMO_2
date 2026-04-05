<template>
  <component
    :is="iconComponent"
    :stroke-width="strokeWidth"
    :size="resolvedSize"
    :style="{ minWidth: resolvedSize + 'px', minHeight: resolvedSize + 'px' }"
    :class="$attrs.class"
  />
</template>

<script setup>
import { computed } from 'vue'
import * as LucideIcons from 'lucide-vue-next'

const props = defineProps({
  name: { type: String, required: true },
  size: { type: [Number, String], default: null },
  strokeWidth: { type: [Number, String], default: 2 }
})

const NAME_MAP = {
  // Navigation & Layout
  dashboard: 'LayoutDashboard',
  quiz: 'FileText',
  list_alt: 'List',
  live_tv: 'Eye',
  assessment: 'BarChart2',
  radar: 'Radar',
  account_circle: 'User',
  home: 'Home',
  menu: 'Menu',
  close: 'X',
  check: 'Check',
  arrow_back: 'ArrowLeft',
  arrow_forward: 'ArrowRight',
  chevron_right: 'ChevronRight',
  chevron_left: 'ChevronLeft',
  expand_more: 'ChevronDown',
  expand_less: 'ChevronUp',
  arrow_drop_down: 'ChevronDown',

  // Actions
  edit: 'Edit3',
  delete: 'Trash2',
  add: 'Plus',
  remove: 'Minus',
  send: 'Send',
  download: 'Download',
  upload: 'Upload',
  print: 'Printer',
  save: 'Save',
  settings: 'Settings',
  search: 'Search',
  search_off: 'SearchX',
  visibility: 'Eye',
  visibility_off: 'EyeOff',
  open_in_new: 'ExternalLink',
  content_copy: 'Copy',
  content_paste: 'Clipboard',
  copy: 'Copy',

  // Status & Feedback
  check_circle: 'CheckCircle',
  check_circle_outline: 'CheckCircle',
  warning: 'AlertTriangle',
  error: 'AlertCircle',
  info: 'Info',
  help: 'HelpCircle',
  error_outline: 'AlertCircle',
  sync: 'RefreshCw',
  sync_disabled: 'RefreshCw',
  radio_button_checked: 'Radio',

  // Media & Recording
  videocam: 'Video',
  videocam_off: 'VideoOff',
  mic: 'Mic',
  mic_off: 'MicOff',
  screenshot: 'Monitor',
  monitor_heart: 'Activity',
  pause: 'Pause',
  pause_circle: 'PauseCircle',
  play_arrow: 'Play',
  block: 'Ban',

  // People & Groups
  group: 'Users',
  person: 'User',
  face: 'UserCheck',
  manage_accounts: 'Users',
  'user-plus': 'UserPlus',
  'users-plus': 'UserPlus',
  'user-minus': 'UserMinus',
  'alert-circle': 'AlertCircle',
  pencil: 'Pencil',
  'pencil-2': 'Pencil',
  'trash-2': 'Trash2',
  book_open: 'BookOpen',
  badge: 'Award',
  cake: 'Cake',
  delete_sweep: 'Trash2',
  format_list_numbered: 'ListOrdered',
  all_inclusive: 'Infinity',
  celebration: 'PartyPopper',

  // Time & Schedule
  schedule: 'Clock',
  timer: 'Timer',
  timer_off: 'TimerOff',
  today: 'Calendar',

  // Security & Proctoring
  shield: 'Shield',
  shield_off: 'ShieldOff',
  security: 'ShieldCheck',
  verified_user: 'ShieldCheck',
  gpp_bad: 'ShieldAlert',

  // Data & Charts
  bar_chart: 'BarChart2',
  bar_chart_2: 'BarChart2',
  show_chart: 'TrendingUp',
  analytics: 'BarChart3',
  insights: 'TrendingUp',
  trending_up: 'TrendingUp',
  trending_down: 'TrendingDown',
  trending_flat: 'Minus',

  // Communication
  mail: 'Mail',
  notifications: 'Bell',
  notifications_off: 'BellOff',
  chat: 'MessageSquare',
  flag: 'Flag',

  // Files & Documents
  description: 'FileText',
  assignment: 'ClipboardList',
  fact_check: 'CheckSquare',
  list: 'List',
  list_alt: 'List',
  feed: 'Rss',

  // Network & Connectivity
  wifi: 'Wifi',
  wifi_off: 'WifiOff',
  x: 'X',
  signal_cellular_off: 'WifiOff',
  router: 'Network',

  // UI & Interface
  more_vert: 'MoreVertical',
  more_horiz: 'MoreHorizontal',
  drag_indicator: 'GripVertical',
  refresh: 'RefreshCw',
  sort: 'ArrowUpDown',
  filter_list: 'Filter',
  fullscreen: 'Maximize',
  display_settings: 'Monitor',
  fullscreen_exit: 'Minimize',

  // Misc
  flash_on: 'Zap',
  bolt: 'Zap',
  code: 'Code',
  terminal: 'Terminal',
  build: 'Wrench',
  folder: 'Folder',
  folder_open: 'FolderOpen',
  image: 'Image',
  picture_as_pdf: 'FileText',
  pie_chart: 'PieChart',
  text_fields: 'Type',
  psychology: 'Brain',
  bookmark: 'Bookmark',
  bookmark_border: 'Bookmark',
  event: 'Calendar',
  calendar_today: 'Calendar',
  language: 'Globe',
  lock: 'Lock',
  lock_open: 'Unlock',
  star: 'Star',
  star_border: 'Star',
  grade: 'Star',
  stars: 'Star',
  favorite: 'Heart',
  favorite_border: 'Heart',
  thumb_up: 'ThumbsUp',
  thumb_down: 'ThumbsDown',
  share: 'Share2',
  link: 'Link',
  public: 'Globe',
  place: 'MapPin',
  contact_phone: 'Phone',
  email: 'Mail',
  phone: 'Phone',
  computer: 'Monitor',
  desktop_windows: 'Monitor',
  tablet: 'Tablet',
  smartphone: 'Smartphone',
  mouse: 'Mouse',
  keyboard: 'Keyboard',
  foggy: 'CloudOff',
  report: 'FileWarning',
  table_chart: 'Table',
  compare_arrows: 'ArrowLeftRight',
  timeline: 'Activity',
  domain: 'Building',
  school: 'GraduationCap',
  book: 'BookOpen',
  book_open: 'BookOpen',
  library_books: 'Library',
  local_fire_department: 'Flame',
  workspace_premium: 'Award',
  emoji_events: 'Trophy',
  sports: 'Dumbbell',
  sports_esports: 'Gamepad2',
  movie: 'Film',
  music_note: 'Music',
  restaurants: 'UtensilsCrossed',
  flight: 'Plane',
  directions_car: 'Car',
  directions_bus: 'Bus',
  train: 'Train',
  local_hospital: 'HeartPulse',
  science: 'FlaskConical',
  agriculture: 'Sprout',
  engineering: 'Cog',
  business_center: 'Briefcase',
  military_tech: 'Shield',
  government: 'Building2',
  real_estate_agent: 'Home',
  construction: 'HardHat',
  shopping_cart: 'ShoppingCart',
  attach_money: 'DollarSign',
  credit_card: 'CreditCard',
  account_balance: 'Landmark',
  savings: 'PiggyBank',
  category: 'LayoutGrid',
  apps: 'AppWindow',
  vpn_key: 'Key',
  storage: 'Database',
  cloud: 'Cloud',
  cloud_upload: 'CloudUpload',
  cloud_off: 'CloudOff',
  backup: 'Archive',
  restore: 'RotateCcw',
  supervisor_account: 'UserCog',
  accessibility: 'Accessibility',
  pets: 'Bird',
  child_care: 'Baby',
  family_restroom: 'Users',
  pregnant_woman: 'Baby',
  self_improvement: 'Heart',
  nightlight: 'Moon',
  wb_sunny: 'Sun',
  eco: 'Leaf',
  speed: 'Gauge',
  tab: 'Square',
  skip_next: 'SkipForward',
  skip_previous: 'SkipBack',
  radio: 'Radio',
  live_tv: 'Tv',
  progressive_pause_circle: 'PauseCircle',
  play_circle: 'PlayCircle',
  stop_circle: 'StopCircle',

  // Custom icons
  waving_hand: 'Hand',
  model_training: 'Brain',
  calendar_month: 'CalendarDays',
  cloud_upload: 'UploadCloud',
  summarize: 'FileText',
  inventory_2: 'Package',
  task_alt: 'CheckCircle',
  class: 'School',
  edit_note: 'FilePen',
  photo_camera: 'Camera',
  tune: 'SlidersHorizontal',
  graduation_cap: 'GraduationCap',
  'graduation-cap': 'GraduationCap',
  groups: 'Users',
  users: 'Users',
  school: 'GraduationCap',
  'chevron-right': 'ChevronRight',
  'chevron-left': 'ChevronLeft',
  'user-check': 'UserCheck',
  'user-x': 'UserX',
  'pending_actions': 'Clock',
  'analytics': 'BarChart3',
  'hourglass_top': 'Hourglass',
  'bar_chart_2': 'BarChart2',
  'alert-triangle': 'AlertTriangle',
  'check-circle': 'CheckCircle',
  'layout_dashboard': 'LayoutDashboard',
  'clipboard_list': 'ClipboardList',
  'message_circle': 'MessageCircle',
  'file_check_2': 'FileCheck2',
  'server_off': 'ServerOff',
  'help_circle': 'HelpCircle',
  'log_out': 'LogOut',
  'shield_check': 'ShieldCheck',
  'co_present': 'Presentation',
  'activity': 'Activity',
  'server': 'Server',
  'refresh': 'RefreshCw',
  'x': 'X',
  'users': 'Users',
  'trending_up': 'TrendingUp',
  'trending_down': 'TrendingDown',
  'presentation': 'Presentation',
  'pending': 'Clock',
  'check_circle': 'CheckCircle',
  'file_text': 'FileText',
  'plus': 'Plus',
  'check': 'Check',
  'event_available': 'CalendarCheck2',
  'visibility': 'Eye',
  'visibility_off': 'EyeOff',
  'expand_more': 'ChevronDown',
  'expand_less': 'ChevronUp',
  'history': 'History',
  'history_edu': 'BookOpen',
  'leaderboard': 'Trophy',
  'trending': 'TrendingUp',
  'trending_flat': 'Minus',
  'cancel': 'XCircle',
  'check_box': 'CheckSquare',
  'check_box_outline_blank': 'Square',
  'radio_button_unchecked': 'Circle',
  'notifications_off': 'BellOff',
  'rocket_launch': 'Rocket',
  'tips_and_updates': 'Lightbulb',
  'lightbulb': 'Lightbulb',
  'support_agent': 'Headphones',
  'event_busy': 'CalendarX2',
  'event_note': 'CalendarHeart',
  'hourglass_empty': 'Clock',
  'hourglass_full': 'Clock',
  'done_all': 'CheckCheck',
  'info': 'Info',
  'done': 'Check',
  'bookmark': 'Bookmark',
  'menu_book': 'BookOpen',
  'arrow_forward': 'ArrowRight',
  'arrow_back': 'ArrowLeft',
  'login': 'LogIn',
  'logout': 'LogOut',
  'account_circle': 'User',
  'upload_file': 'Upload',
  'download': 'Download',
  'star': 'Star',
  'waving_hand': 'Hand',
  'hash': 'Hash',
}

const resolvedSize = computed(() => {
  if (props.size === null) return 20
  const n = Number(props.size)
  return isNaN(n) ? 20 : n
})

// Cache icons to avoid repeated lookups - optimization for performance
const iconCache = new Map()

const iconComponent = computed(() => {
  // Check cache first for performance
  if (iconCache.has(props.name)) {
    return iconCache.get(props.name)
  }

  const name = NAME_MAP[props.name] || props.name
  const icon = LucideIcons[name] || LucideIcons.Circle

  // Cache the resolved icon
  iconCache.set(props.name, icon)

  return icon
})
</script>

<script>
export default {
  name: 'LucideIcon'
}
</script>
