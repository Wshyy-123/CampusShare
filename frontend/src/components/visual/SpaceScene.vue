<script setup lang="ts">
import type { SpaceKind } from "../../types/campus";
defineProps<{ kind: SpaceKind; variant: number }>();
</script>

<template>
  <svg
    class="space-scene"
    viewBox="0 0 480 220"
    fill="none"
    aria-hidden="true"
    :class="kind.toLowerCase()"
  >
    <defs>
      <linearGradient
        :id="`wall-${variant}`"
        x1="0"
        y1="0"
        x2="480"
        y2="240"
        gradientUnits="userSpaceOnUse"
      >
        <stop stop-color="currentColor" stop-opacity=".13" />
        <stop offset="1" stop-color="currentColor" stop-opacity=".02" />
      </linearGradient>
      <radialGradient :id="`light-${variant}`">
        <stop stop-color="currentColor" stop-opacity=".22" />
        <stop offset="1" stop-color="currentColor" stop-opacity="0" />
      </radialGradient>
    </defs>
    <rect width="480" height="220" fill="#141720" />
    <rect width="480" height="220" :fill="`url(#wall-${variant})`" />
    <ellipse
      cx="330"
      cy="70"
      rx="200"
      ry="160"
      :fill="`url(#light-${variant})`"
    />
    <g v-if="kind === 'LIBRARY_SEAT'" stroke="currentColor" stroke-width="1.1">
      <path
        d="M0 168L480 150 M0 220L480 168 M85 220L280 151 M325 220L390 153"
        opacity=".15"
      />
      <path
        d="M270 24L423 39V133L270 139Z"
        fill="currentColor"
        fill-opacity=".045"
        stroke-opacity=".35"
      />
      <path d="M320 29v108 M370 34v100 M270 80l153 5" opacity=".22" />
      <path
        d="M42 54L150 41V161L42 176Z M42 87l108-14 M42 119l108-14 M42 151l108-14"
        stroke-opacity=".35"
      />
      <g opacity=".4">
        <path
          v-for="n in 12"
          :key="n"
          :d="`M${49 + n * 7} ${62 - n}v17 M${49 + n * 7} ${95 - n}v17 M${49 + n * 7} ${128 - n}v17`"
          stroke-width="4"
        />
      </g>
      <path
        d="M172 132l113-12 79 29-114 15Z"
        fill="currentColor"
        fill-opacity=".1"
        stroke-opacity=".7"
      />
      <path
        d="M183 138v48 M352 153v42 M251 164v42 M283 123V99l-12-13 M268 89h16"
        stroke-opacity=".65"
      />
      <path
        d="M221 167l-22-7v24l23 8 22-4v-24 M200 184v18 M242 188v20"
        fill="#1e2531"
        stroke-opacity=".45"
      />
      <path
        d="M387 159v-42 M387 142c-24-2-26-24-26-24s26-1 26 24 M387 130c25-4 27-26 27-26s-26 2-27 26 M376 158h23l-4 23h-15Z"
        fill="currentColor"
        fill-opacity=".1"
        stroke-opacity=".45"
      />
      <path
        d="M292 138l20-2 15 6-20 2Z"
        fill="currentColor"
        fill-opacity=".25"
        stroke="none"
      />
    </g>
    <g
      v-else-if="kind === 'CLASSROOM'"
      stroke="currentColor"
      stroke-width="1.1"
    >
      <path d="M0 169l480-18 M100 220l150-64 M380 220l-40-65" opacity=".15" />
      <path
        d="M145 40h195v86H145z"
        fill="currentColor"
        fill-opacity=".045"
        stroke-opacity=".5"
      />
      <path
        d="M154 49h177v64H154z M197 68h88 M215 79h52 M207 94h68"
        stroke-opacity=".28"
      />
      <path d="M238 24h11v15 M111 126h263" stroke-opacity=".4" />
      <g
        v-for="row in 3"
        :key="row"
        :transform="`translate(${-row * 15} ${row * 20})`"
      >
        <g
          v-for="col in 4"
          :key="col"
          :transform="`translate(${col * 66} 0)`"
          stroke-opacity=".4"
        >
          <path
            d="M25 116h43l18 9H39Z"
            fill="currentColor"
            fill-opacity=".08"
          />
          <path d="M40 126v12 M81 127v12 M43 137h23v-9H43z" />
        </g>
      </g>
      <path d="M375 45l48-6v97l-48 5Z M399 42v97 M375 89l48-4" opacity=".35" />
      <path
        d="M52 55l50 9v72l-50-6Z M69 58v74 M85 61v74 M52 91l50 7"
        opacity=".2"
      />
    </g>
    <g v-else stroke="currentColor" stroke-width="1.2">
      <path d="M0 220L165 71h150l165 149" opacity=".13" />
      <path
        d="M90 190l91-98h126l89 98Z"
        fill="currentColor"
        fill-opacity=".055"
        stroke-opacity=".7"
      />
      <path
        d="M111 170h265 M148 130h193 M191 92l-53 98 M298 92l51 98 M244 92v98"
        opacity=".4"
      />
      <path
        d="M135 155v-40 M350 155v-40 M135 119h215 M135 125h215 M135 131h215 M135 137h215"
        stroke-opacity=".5"
      />
      <path d="M135 119l215 18 M135 137l215-18" opacity=".2" />
      <path
        d="M159 119v18 M184 119v18 M209 119v18 M234 119v18 M259 119v18 M284 119v18 M309 119v18 M334 119v18"
        opacity=".23"
      />
      <path d="M70 40h340 M105 40v16 M240 40v16 M375 40v16" opacity=".2" />
      <path d="M92 57h27 M227 57h27 M362 57h27" stroke-width="3" opacity=".5" />
      <path d="M40 198V76 M440 198V76 M27 200h27 M427 200h27" opacity=".16" />
    </g>
    <text
      x="24"
      y="31"
      fill="currentColor"
      opacity=".5"
      font-family="monospace"
      font-size="9"
      letter-spacing="3"
    >
      {{
        kind === "LIBRARY_SEAT"
          ? "FOCUS & READ"
          : kind === "CLASSROOM"
            ? "LEARN & CREATE"
            : "MOVE & PLAY"
      }}
    </text>
    <text
      x="456"
      y="200"
      text-anchor="end"
      fill="currentColor"
      opacity=".35"
      font-family="monospace"
      font-size="10"
    >
      0{{ variant }} / CAMPUS SPACES
    </text>
  </svg>
</template>
