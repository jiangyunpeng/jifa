<!--
    Copyright (c) 2023 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<script setup lang="ts">
import { availableLocales, currentLocale, setLocale } from '@/i18n/i18n';
import { isDark } from '@/composables/theme';
import Sun from '@/components/icons/Sun.vue';
import Moon from '@/components/icons/Moon.vue';
import Logout from 'vue-material-design-icons/Logout.vue';
import Translate from 'vue-material-design-icons/Translate.vue';
import Github from 'vue-material-design-icons/Github.vue';
import eclipseLogoUrl from '@/assets/eclipse_incubation_horizontal.svg';
import eclipseLogoUrlForDark from '@/assets/eclipse_incubation_horizontal_dark.svg';
import { t } from '@/i18n/i18n';
import { useEnv } from '@/stores/env';
import { useHeaderToolbar } from '@/composables/header-toolbar';

const logo = computed(() => (isDark.value ? eclipseLogoUrlForDark : eclipseLogoUrl));

const env = useEnv();

const localeOptions = computed(() => {
  let result = {};
  Object.assign(result, availableLocales);
  delete result[currentLocale.value];
  return result;
});

const themeSwitchColor = computed(() => (isDark.value ? '#1a1a1a' : '#ffffff'));

const { toolbar } = useHeaderToolbar();
</script>
<template>
  <header class="header">
    <div class="container">
      <div class="left-side">
        <img style="height: calc(var(--ej-header-height) - 4px)" alt="" :src="logo" />
        <span class="project-name">Jifa</span>

        <template v-if="toolbar">
          <component :is="toolbar" />
        </template>
      </div>

      <div class="right-side">
        <el-popover
          placement="bottom"
          :show-arrow="false"
          :popper-style="{ 'min-width': '90px', width: '90px', padding: '5px 0' }"
          v-if="env.loggedIn"
        >
          <template #reference>
            <el-text style="cursor: pointer">{{ env.user?.name }}</el-text>
          </template>
          <template #default>
            <div class="popover-item" @click="env.logout()">
              <Logout :size="18" style="margin-right: 5px; height: 18px" />
              {{ t('header.logout') }}
            </div>
          </template>
        </el-popover>

        <el-button
          link
          tag="a"
          class="ej-header-button"
          @click="env.loginFormVisible = true"
          v-else
        >
          {{ t('header.login') }}
        </el-button>

        <el-divider direction="vertical" />

        <el-popover
          placement="bottom"
          :show-arrow="false"
          :popper-style="{ 'min-width': '100px', width: '100px', padding: '5px 0' }"
        >
          <template #reference>
            <Translate
              :size="20"
              style="height: 20px; color: var(--el-text-color-regular); cursor: pointer"
            />
          </template>
          <template #default>
            <div class="popover-item" v-for="(label, key) in localeOptions" @click="setLocale(key)">
              {{ label }}
            </div>
          </template>
        </el-popover>

        <el-divider direction="vertical" />

        <el-switch
          class="theme-switch"
          v-model="isDark"
          :active-action-icon="Moon"
          :inactive-action-icon="Sun"
        />

        <el-divider direction="vertical" />

        <a href="https://github.com/eclipse/jifa" target="_blank" style="height: 24px">
          <Github class="github-icon" title="Github" :size="24" />
        </a>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: var(--ej-header-height);
  overflow: hidden;
  border-bottom: 1px solid var(--el-border-color);
  background-color: var(--el-bg-color);
}

.container {
  height: 100%;
  display: flex;
  justify-content: space-between;
}

.left-side {
  display: flex;
  align-items: center;
}

.project-name {
  color: var(--el-text-color-primary);
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 3px;
  cursor: default;
}

.right-side {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-right: 14px;
}

.popover-item {
  padding: 5px;
  text-align: center;
  color: var(--el-text-color-regular);
  cursor: pointer;
  display: flex;
  flex-direction: row;
  justify-content: center;
  align-items: center;
}

:deep(.el-divider--vertical) {
  margin: 0 14px !important;
}

.popover-item:hover {
  color: var(--el-menu-hover-text-color);
  background-color: var(--el-menu-hover-bg-color);
}

.theme-switch {
  --el-switch-on-color: #323238;
}

.theme-switch :deep(.el-switch__action) {
  background-color: v-bind(themeSwitchColor);
}

.github-icon {
  color: var(--el-text-color-regular);
}

.github-icon:hover {
  color: var(--el-text-color-primary);
}
</style>
