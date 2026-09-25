import React, { useLayoutEffect, useRef } from "react";

export const QUAD_VERTEX = `#version 300 es
in vec2 aPos;
out vec2 vUv;
void main() {
  vUv = aPos * 0.5 + 0.5;
  gl_Position = vec4(aPos, 0.0, 1.0);
}`;

type Uniform = number | [number, number] | [number, number, number] | [number, number, number, number] | Float32Array;

export class Gl {
  readonly gl: WebGL2RenderingContext;
  private programs = new Map<string, WebGLProgram>();
  private buffers = new Map<string, WebGLBuffer>();
  private textures = new Map<string, WebGLTexture>();
  private vaos = new Map<string, WebGLVertexArrayObject>();
  private quad: WebGLBuffer;

  constructor(gl: WebGL2RenderingContext) {
    this.gl = gl;
    gl.getExtension("EXT_color_buffer_float");
    gl.getExtension("OES_texture_float_linear");
    this.quad = gl.createBuffer()!;
    gl.bindBuffer(gl.ARRAY_BUFFER, this.quad);
    gl.bufferData(gl.ARRAY_BUFFER, new Float32Array([-1, -1, 1, -1, -1, 1, -1, 1, 1, -1, 1, 1]), gl.STATIC_DRAW);
  }

  program(key: string, vertex: string, fragment: string): WebGLProgram {
    const existing = this.programs.get(key);
    if (existing) return existing;
    const gl = this.gl;
    const compile = (type: number, source: string) => {
      const shader = gl.createShader(type)!;
      gl.shaderSource(shader, source);
      gl.compileShader(shader);
      if (!gl.getShaderParameter(shader, gl.COMPILE_STATUS)) {
        throw new Error(`${key} shader: ${gl.getShaderInfoLog(shader)}`);
      }
      return shader;
    };
    const program = gl.createProgram()!;
    gl.attachShader(program, compile(gl.VERTEX_SHADER, vertex));
    gl.attachShader(program, compile(gl.FRAGMENT_SHADER, fragment));
    gl.linkProgram(program);
    if (!gl.getProgramParameter(program, gl.LINK_STATUS)) {
      throw new Error(`${key} link: ${gl.getProgramInfoLog(program)}`);
    }
    this.programs.set(key, program);
    return program;
  }

  buffer(key: string, data: Float32Array | (() => Float32Array), usage: number = WebGL2RenderingContext.STATIC_DRAW): WebGLBuffer {
    const gl = this.gl;
    let buffer = this.buffers.get(key);
    if (!buffer) {
      buffer = gl.createBuffer()!;
      gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
      gl.bufferData(gl.ARRAY_BUFFER, typeof data === "function" ? data() : data, usage);
      this.buffers.set(key, buffer);
    } else if (usage === gl.DYNAMIC_DRAW && typeof data !== "function") {
      gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
      gl.bufferData(gl.ARRAY_BUFFER, data, usage);
    }
    return buffer;
  }

  texture(key: string, source: TexImageSource, options: { repeatX?: boolean; mipmap?: boolean } = {}): WebGLTexture {
    const existing = this.textures.get(key);
    if (existing) return existing;
    const gl = this.gl;
    const texture = gl.createTexture()!;
    gl.bindTexture(gl.TEXTURE_2D, texture);
    gl.pixelStorei(gl.UNPACK_FLIP_Y_WEBGL, false);
    gl.pixelStorei(gl.UNPACK_COLORSPACE_CONVERSION_WEBGL, gl.NONE);
    gl.pixelStorei(gl.UNPACK_PREMULTIPLY_ALPHA_WEBGL, false);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA, gl.RGBA, gl.UNSIGNED_BYTE, source);
    if (options.mipmap) gl.generateMipmap(gl.TEXTURE_2D);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, options.mipmap ? gl.LINEAR_MIPMAP_LINEAR : gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.LINEAR);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, options.repeatX ? gl.REPEAT : gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    this.textures.set(key, texture);
    return texture;
  }

  floatTexture(key: string, width: number, height: number, data: () => Float32Array, repeatX = true): WebGLTexture {
    const existing = this.textures.get(key);
    if (existing) return existing;
    const gl = this.gl;
    const texture = gl.createTexture()!;
    gl.bindTexture(gl.TEXTURE_2D, texture);
    gl.texImage2D(gl.TEXTURE_2D, 0, gl.RGBA32F, width, height, 0, gl.RGBA, gl.FLOAT, data());
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MIN_FILTER, gl.NEAREST);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_MAG_FILTER, gl.NEAREST);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_S, repeatX ? gl.REPEAT : gl.CLAMP_TO_EDGE);
    gl.texParameteri(gl.TEXTURE_2D, gl.TEXTURE_WRAP_T, gl.CLAMP_TO_EDGE);
    this.textures.set(key, texture);
    return texture;
  }

  use(program: WebGLProgram, uniforms: Record<string, Uniform>, textures: Record<string, WebGLTexture> = {}) {
    const gl = this.gl;
    gl.useProgram(program);
    for (const [name, value] of Object.entries(uniforms)) {
      const location = gl.getUniformLocation(program, name);
      if (location === null) continue;
      if (typeof value === "number") gl.uniform1f(location, value);
      else if (value instanceof Float32Array) {
        if (value.length === 9) gl.uniformMatrix3fv(location, false, value);
        else if (value.length === 16) gl.uniformMatrix4fv(location, false, value);
        else gl.uniform1fv(location, value);
      } else if (value.length === 2) gl.uniform2f(location, value[0], value[1]);
      else if (value.length === 3) gl.uniform3f(location, value[0], value[1], value[2]);
      else gl.uniform4f(location, value[0], value[1], value[2], value[3]);
    }
    let unit = 0;
    for (const [name, texture] of Object.entries(textures)) {
      gl.activeTexture(gl.TEXTURE0 + unit);
      gl.bindTexture(gl.TEXTURE_2D, texture);
      gl.uniform1i(gl.getUniformLocation(program, name), unit);
      unit++;
    }
  }

  drawQuad(program: WebGLProgram) {
    const gl = this.gl;
    const location = gl.getAttribLocation(program, "aPos");
    gl.bindBuffer(gl.ARRAY_BUFFER, this.quad);
    gl.enableVertexAttribArray(location);
    gl.vertexAttribPointer(location, 2, gl.FLOAT, false, 0, 0);
    gl.drawArrays(gl.TRIANGLES, 0, 6);
  }

  attribute(program: WebGLProgram, name: string, buffer: WebGLBuffer, size: number) {
    const gl = this.gl;
    const location = gl.getAttribLocation(program, name);
    if (location < 0) return;
    gl.bindBuffer(gl.ARRAY_BUFFER, buffer);
    gl.enableVertexAttribArray(location);
    gl.vertexAttribPointer(location, size, gl.FLOAT, false, 0, 0);
  }

  vao(key: string): WebGLVertexArrayObject {
    let vao = this.vaos.get(key);
    if (!vao) {
      vao = this.gl.createVertexArray()!;
      this.vaos.set(key, vao);
    }
    return vao;
  }

  clear(r = 0, g = 0, b = 0, a = 0) {
    const gl = this.gl;
    gl.viewport(0, 0, gl.drawingBufferWidth, gl.drawingBufferHeight);
    gl.clearColor(r, g, b, a);
    gl.clear(gl.COLOR_BUFFER_BIT);
  }

  blend(mode: "additive" | "alpha" | "none") {
    const gl = this.gl;
    if (mode === "none") {
      gl.disable(gl.BLEND);
      return;
    }
    gl.enable(gl.BLEND);
    if (mode === "additive") gl.blendFunc(gl.ONE, gl.ONE);
    else gl.blendFunc(gl.ONE, gl.ONE_MINUS_SRC_ALPHA);
  }
}

type GlCanvasProps = {
  width: number;
  height: number;
  draw: (gl: Gl) => void;
  style?: React.CSSProperties;
};

export const GlCanvas: React.FC<GlCanvasProps> = ({ width, height, draw, style }) => {
  const canvas = useRef<HTMLCanvasElement>(null);
  const context = useRef<Gl | null>(null);
  useLayoutEffect(() => {
    const element = canvas.current;
    if (!element) return;
    if (!context.current) {
      const gl = element.getContext("webgl2", { premultipliedAlpha: true, preserveDrawingBuffer: true, antialias: true, alpha: true });
      if (!gl) throw new Error("WebGL2 unavailable");
      context.current = new Gl(gl);
    }
    draw(context.current);
  });
  return <canvas ref={canvas} width={width} height={height} style={{ position: "absolute", left: 0, top: 0, width, height, ...style }} />;
};
